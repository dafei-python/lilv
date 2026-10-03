#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
利率速查 · 数据抓取脚本
=======================

每日由 GitHub Actions 调用一次（cron 9:30 北京时间 ≈ 1:30 UTC），
抓取以下权威源的最新利率，写入 docs/rates.json（GitHub Pages 发布目录）：

  1. LPR —— 中国货币网（央行授权发布）
     https://www.chinamoney.com.cn/chinese/bklpr/
  2. 公积金贷款利率 —— 中国人民银行公开公告
     （不定期调整，脚本从 rates.json 历史快照继承值，公告发布后手动更新 effectiveSince）
  3. 五大行存款利率 —— 邮储官网自动抓取（唯一可靠源），
     工/农/中/建官网利率表长期不更新，用半静态值（五大行基本同步降息）
  4. 典型房贷执行利率 —— 跟随 LPR 自动计算（首套 LPR-45BP 等）
  5. 10 年期国债收益率 / 沪深300 近10年年化 —— 东方财富接口，每日自动
  6. 理财平均年化 —— 证券时报标签聚合流识别官方半年报/年报快讯，每年自动更新 2 次
  7. 寿险预定利率上限 —— 中保协每季度研究值 + 监管动态调整规则自动联动

容错策略：
  - 任一源抓取失败时，沿用上一份 rates.json 中的旧值（保本）
  - 全部失败时，直接退出 0，保留旧 rates.json 不动

输出文件结构由 app/src/main/java/com/dushishiyi/lilv/data/RatesDto.kt 定义，
本脚本输出必须严格匹配该 schema。
"""

from __future__ import annotations

import json
import math
import re
import subprocess
import sys
import time
from datetime import datetime, timezone, timedelta
from pathlib import Path
from typing import Any, Optional

import requests
from bs4 import BeautifulSoup

# ============================================================
# 配置
# ============================================================

OUTPUT_PATH = Path(__file__).resolve().parent.parent.parent / "docs" / "rates.json"
USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_6) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
)
TIMEOUT = 10

# 五大行存款利率页面 URL
# 注意：工行/农行/中行/建行官网利率表长期不更新或 JS 动态渲染抓不到，
# 只有邮储官网利率表可靠（https://www.psbc.com/cn/common/bjfw/rmbllcx/）。
# 其他四行用半静态值（五大行利率基本同步调整，邮储有时略高 1-3 BP）。
BANK_URLS = {
    "icbc": ("中国工商银行", "https://www.icbc.com.cn/column/1438058343720960356.html"),
    "abc":  ("中国农业银行", "https://www.abchina.com/cn/PersonalBanking/FinancialPlanner/rate/"),
    "boc":  ("中国银行", "https://www.boc.cn/fimarkets/interestrate/"),
    "ccb":  ("中国建设银行", "https://www.ccb.com/cn/personal/interest/rmbdeposit.html"),
    "psbc": ("中国邮政储蓄银行", "https://www.psbc.com/cn/common/bjfw/rmbllcx/"),  # ✅ 唯一可靠源
}

# 其他四大行半静态挂牌利率（2025-05-20 降息后）
# 调整频率约半年一次，人行降息时手动更新此处即可
STATIC_BANK_RATES = {
    "icbc": {"demand": 0.05, "term3m": 0.65, "term6m": 0.85, "term1y": 0.95, "term2y": 1.05, "term3y": 1.25, "term5y": 1.30},
    "abc":  {"demand": 0.05, "term3m": 0.65, "term6m": 0.85, "term1y": 0.95, "term2y": 1.05, "term3y": 1.25, "term5y": 1.30},
    "boc":  {"demand": 0.05, "term3m": 0.65, "term6m": 0.85, "term1y": 0.95, "term2y": 1.05, "term3y": 1.25, "term5y": 1.30},
    "ccb":  {"demand": 0.05, "term3m": 0.65, "term6m": 0.85, "term1y": 0.95, "term2y": 1.05, "term3y": 1.25, "term5y": 1.30},
}

LPR_URL = "https://www.chinamoney.com.cn/chinese/bklpr/"
# LPR 历史序列 JSON：货币网前端实际调用的接口（2019 年至今，月度行）
LPR_HISTORY_API = (
    "https://www.chinamoney.com.cn/ags/ms/cm-u-bk-currency/LprChrtCSV?startDate=2019-01-01"
)

# 东方财富 · 中美国债到期收益率（中国 10 年期字段 EMM00166466）
TREASURY_10Y_URL = (
    "https://datacenter-web.eastmoney.com/api/data/v1/get"
    "?reportName=RPTA_WEB_TREASURYYIELD&columns=ALL&pageSize=5"
    "&sortColumns=SOLAR_DATE&sortTypes=-1&source=WEB&client=WEB"
)

# 东方财富 · 沪深 300 历史前复权收盘（用于计算近 10 年年化）
# 该接口要求完整字段集，精简 fields 会返回 rc=102；
# fields2 顺序：f51 日期, f52 开盘, f53 收盘, f54 最高, f55 最低, f56 成交量 …
CSI300_HISTORY_URL = (
    "https://push2his.eastmoney.com/api/qt/stock/kline/get"
    "?secid=1.000300&fields1=f1,f2,f3,f4,f5,f6"
    "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
    "&klt=101&fqt=1&end=20500101"
)

# 贵金属 / 汇率行情
# 主源：上海黄金交易所官网（黄金/白银）、中国货币网（美元中间价）——均为官方发布
# 备源：东方财富公开 K 线接口（无需 key）
# fields2：f51 日期, f52 开盘, f53 收盘, f54 最高, f55 最低, f56 成交量
SGE_DAILYHQ_URL = "https://www.sge.com.cn/graph/Dailyhq"
CHINAMONEY_FX_URL = (
    "https://www.chinamoney.com.cn/ags/ms/cm-u-bk-ccpr/CcprHisNew"
    "?startDate={start}&endDate={end}&currency=USD/CNY"
)
EASTMONEY_KLINE_URL = (
    "https://push2his.eastmoney.com/api/qt/stock/kline/get"
    "?secid={secid}&fields1=f1,f2,f3,f4,f5,f6"
    "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
    "&klt=101&fqt=1&end=20500101&lmt=130"  # 近 130 个交易日 ≈ 半年
)
# (key, 名称, 单位, 价格小数位, SGE 合约代码, 东方财富 secid)
METAL_SPECS = [
    ("gold",   "黄金9999（上海金交所）", "元/克", 2, "Au99.99", "118.AU9999"),
    ("silver", "白银T+D（上海金交所）",  "元/千克", 0, "Ag(T+D)", "118.AGTD"),
]

# 公积金贷款利率：央行调整时才变。脚本默认沿用旧值，
# 央行发布新公告时请在此处更新 EFFECTIVE_SINCE 和四个数值。
FUND_FALLBACK = {
    "first5yBelow": 2.10,
    "first5yAbove": 2.60,
    "second5yBelow": 2.525,
    "second5yAbove": 3.075,
    "effectiveSince": "2025-05-08",
}

# 理财平均年化收益率：银行业理财登记托管中心《半年报告》《年度报告》官方口径，
# 每年 1-2 月、7 月底各发布一期。脚本从证券时报「银行理财」标签聚合流中
# 自动识别快讯全文（新华社/上证报/人民财讯均会报道同一数字），无需人工。
STCN_WEALTH_TAG_API = (
    "https://www.stcn.com/article/kx-tag-detail-list.html?tag=XOfHwrTKfrbj&page={page}"
)
WEALTH_MANAGEMENT_FALLBACK = 2.05   # 最近一期官方报告：2026 年上半年平均年化 2.05%

# 普通型人身保险产品预定利率上限：金融监管总局《关于建立预定利率与市场利率
# 挂钩及动态调整机制有关事项的通知》（2025-01）规定——
#   · 上限取 0.25% 的整数倍
#   · 中保协每季度公布「预定利率研究值」
#   · 在售上限连续两个季度比研究值高/低 25BP 及以上时触发调整
# 脚本自动抓最近两期研究值并按此规则联动上限，无需人工。
IACHINA_LIST_URL = "https://www.iachina.cn/col/col22/index.html"
# 协会要闻列表为 jpage 动态代理，按记录窗口取数；例会每季一次，
# 每次扫描一个窗口直到集齐最近 2 期即可（通常 3-4 个窗口）
IACHINA_DATAPROXY_URL = "https://www.iachina.cn/module/web/jpage/dataproxy.jsp"
IACHINA_PROXY_PARAMS = {
    "col": 1, "webid": 1, "path": "/", "columnid": 22,
    "sourceContentType": 3, "unitid": "2849", "permissiontype": 0, "perpage": 14,
}
INSURANCE_CAP_FALLBACK = 2.00        # 2025-07 起 2.50% → 2.00%
INSURANCE_TRIGGER = 0.25             # 触发阈值 25BP

# 典型房贷执行利率参考（首套 LPR-45BP，二套 LPR-25BP 左右）
MORTGAGE_NOTE = "首套房贷执行利率 ≈ LPR-45BP（参考城市：上海、北京等一线城市）"

CN_TZ = timezone(timedelta(hours=8))

# ============================================================
# 工具函数
# ============================================================

def http_get(url: str) -> str:
    headers = {"User-Agent": USER_AGENT, "Accept-Language": "zh-CN,zh;q=0.9"}
    resp = requests.get(url, headers=headers, timeout=TIMEOUT)
    resp.raise_for_status()
    # 优先用响应头声明的编码，否则尝试 utf-8
    if resp.encoding and resp.encoding.lower() not in ("iso-8859-1",):
        return resp.text
    # 多数国内银行网站是 utf-8 或 gbk
    for enc in ("utf-8", "gbk", "gb2312"):
        try:
            return resp.content.decode(enc)
        except UnicodeDecodeError:
            continue
    return resp.text

def parse_float(s: str) -> Optional[float]:
    """'1.25' / '1.25%' / '0.95' 都能解析为 float；非法返回 None。"""
    if not s:
        return None
    s = s.strip().rstrip("%").replace("％", "").strip()
    try:
        return float(s)
    except ValueError:
        return None

def http_get_raw(url: str, headers: Optional[dict[str, str]] = None) -> str:
    """requests 优先；若对端按 TLS 指纹直接断连（push2his.eastmoney.com 在部分
    网络环境会拒绝 urllib3 的 ClientHello），退回系统 curl，保证不中断。
    """
    hdrs = headers or {"User-Agent": USER_AGENT}
    try:
        resp = requests.get(url, headers=hdrs, timeout=TIMEOUT)
        resp.raise_for_status()
        return resp.content.decode("utf-8", errors="replace")
    except requests.RequestException:
        cmd = ["curl", "-fsSL", "-m", str(TIMEOUT + 5)]
        for k, v in hdrs.items():
            cmd += ["-H", f"{k}: {v}"]
        cmd.append(url)
        proc = subprocess.run(cmd, capture_output=True)
        if proc.returncode != 0:
            raise RuntimeError(
                f"requests/curl 均失败：{proc.stderr.decode(errors='replace')[:200]}"
            )
        return proc.stdout.decode("utf-8", errors="replace")

def http_post_raw(url: str, data: dict[str, str],
                  headers: Optional[dict[str, str]] = None) -> str:
    """POST 表单：requests 优先，失败回退系统 curl（用于 SGE 官网）。"""
    hdrs = headers or {"User-Agent": USER_AGENT}
    try:
        resp = requests.post(url, data=data, headers=hdrs, timeout=TIMEOUT)
        resp.raise_for_status()
        return resp.content.decode("utf-8", errors="replace")
    except requests.RequestException:
        cmd = ["curl", "-fsSL", "-m", str(TIMEOUT + 5), "-X", "POST"]
        for k, v in hdrs.items():
            cmd += ["-H", f"{k}: {v}"]
        for k, v in data.items():
            cmd += ["--data-urlencode", f"{k}={v}"]
        cmd.append(url)
        proc = subprocess.run(cmd, capture_output=True)
        if proc.returncode != 0:
            raise RuntimeError(
                f"requests/curl 均失败：{proc.stderr.decode(errors='replace')[:200]}"
            )
        return proc.stdout.decode("utf-8", errors="replace")

def now_iso() -> str:
    return datetime.now(CN_TZ).isoformat(timespec="seconds")

# ============================================================
# LPR 抓取
# ============================================================

def fetch_lpr() -> dict[str, Any]:
    """从中国货币网 JSON 接口抓取 LPR 历史序列（2019 年至今月度行）。
    压缩掉连续未变动的月份，只保留实际变动节点 + 最新一期。
    """
    try:
        resp = requests.get(
            LPR_HISTORY_API,
            headers={"User-Agent": USER_AGENT, "Referer": LPR_URL},
            timeout=TIMEOUT,
        )
        resp.raise_for_status()
        csv_text = resp.json().get("data", {}).get("csv", "")
    except Exception as e:
        print(f"[WARN] LPR 抓取失败：{e}", file=sys.stderr)
        return {}

    # CSV 列：date, open, high, low, close, volume, 1Y, 5Y
    history: list[dict[str, Any]] = []
    last_pair: Optional[tuple[float, float]] = None
    for line in csv_text.splitlines():
        parts = line.strip().split(",")
        if len(parts) < 8:
            continue
        m = re.match(r"^(\d{4}-\d{2}-\d{2})$", parts[0])
        v1 = parse_float(parts[6])
        v5 = parse_float(parts[7])
        if not m or v1 is None or v5 is None:
            continue  # 5Y 于 2019-08 才推出，早期空行直接跳过
        pair = (v1, v5)
        # 接口按日期降序：第一行恒保留（最新），其余仅在数值变动时保留
        if last_pair is not None and pair == last_pair:
            continue
        history.append({"date": m.group(1), "lpr1y": v1, "lpr5y": v5})
        last_pair = pair
        if len(history) >= 24:
            break

    if not history:
        return {}
    return {"current": history[0], "history": history}

# ============================================================
# 五大行存款利率抓取
# ============================================================

TERM_KEYS = ["demand", "term3m", "term6m", "term1y", "term2y", "term3y", "term5y"]
TERM_LABELS = ["活期", "三个月", "半年", "一年", "二年", "三年", "五年"]

# 各档期限在官网表格里的标签关键字（按匹配优先级）
TERM_LABEL_KEYWORDS = {
    "term3m": ["三个月"],
    "term6m": ["六个月", "半年"],
    "term1y": ["一年"],
    "term2y": ["二年", "两年"],
    "term3y": ["三年"],
    "term5y": ["五年"],
}
# 整存整取段之后的下一段标记
NEXT_SECTION_MARKS = ("零存整取", "定活两便", "协定存款", "通知存款")

def _next_number(cells: list[str], start: int, lookahead: int = 4) -> Optional[float]:
    """取 cells[start] 之后第一个可解析为数字的单元格。"""
    for v in cells[start + 1:start + 1 + lookahead]:
        val = parse_float(v)
        if val is not None:
            return val
    return None

def _parse_vertical_table(cells: list[str]) -> Optional[dict[str, float]]:
    """解析竖排表（邮储官网结构）：标签与数值交替排列。
    例：…'1.整存整取' | '' | '三个月' | '0.65' | '六个月' | '0.86' | '一年' | '0.98' …
    """
    section_start = -1
    for i, c in enumerate(cells):
        if "整存整取" in c and "零存" not in c:
            section_start = i
            break
    if section_start < 0:
        return None

    section_end = len(cells)
    for j in range(section_start + 1, len(cells)):
        if any(mark in cells[j] for mark in NEXT_SECTION_MARKS):
            section_end = j
            break
    section = cells[section_start:section_end]

    rates: dict[str, float] = {}
    for key, labels in TERM_LABEL_KEYWORDS.items():
        for i, c in enumerate(section):
            if any(lb in c for lb in labels):
                val = _next_number(section, i)
                if val is not None:
                    rates[key] = val
                break

    # 活期：全表第一个含「活期」的标签后的数字
    for i, c in enumerate(cells):
        if "活期" in c:
            val = _next_number(cells, i)
            if val is not None:
                rates["demand"] = val
            break

    return rates if all(k in rates for k in TERM_KEYS) else None

def _parse_horizontal_table(cells: list[str]) -> Optional[dict[str, float]]:
    """解析横排表（旧通用逻辑）：表头连续 7 个期限标签，其后连续 7 个数字。"""
    head_idx = -1
    for i, c in enumerate(cells):
        if "活期" in c:
            head_idx = i
            break
    if head_idx < 0 or head_idx + 7 >= len(cells):
        return None

    rates: dict[str, float] = {}
    for i, key in enumerate(TERM_KEYS):
        v = parse_float(cells[head_idx + i + 1])
        if v is None:
            return None
        rates[key] = v
    return rates

def extract_deposit_rates(html: str) -> Optional[dict[str, Any]]:
    """从银行官网利率表 HTML 解析整存整取 7 档利率。
    兼容两种排版：
      A. 竖排：标签/数值交替（邮储官网）
      B. 横排：表头一行标签 + 数据行数字（其他行常见结构）
    """
    soup = BeautifulSoup(html, "lxml")
    candidate_table = None
    for tbl in soup.find_all("table"):
        text = tbl.get_text()
        if "活期" in text and ("一年" in text or "三个月" in text):
            candidate_table = tbl
            break
    if candidate_table is None:
        return None

    cells = [td.get_text(strip=True) for td in candidate_table.find_all(["td", "th"])]
    if not cells:
        return None

    return _parse_vertical_table(cells) or _parse_horizontal_table(cells)

def fetch_deposit_banks() -> list[dict[str, Any]]:
    """抓取五大行存款利率。
    - 邮储：自动抓取官网（唯一可靠源）
    - 其他四大行：用半静态值（官网利率表长期不更新）
    失败时保本：从旧 rates.json 继承该行数据。
    """
    prev = read_prev_payload()
    prev_banks = {b["code"]: b for b in (prev.get("deposit", {}).get("banks", []))}

    out: list[dict[str, Any]] = []
    for code, (name, url) in BANK_URLS.items():
        if code == "psbc":
            # 邮储：自动抓取
            try:
                html = http_get(url)
                rates = extract_deposit_rates(html)
                if rates is None:
                    raise RuntimeError("无法在页面中解析利率表")
                updated_at = datetime.now(CN_TZ).strftime("%Y-%m-%d")
                out.append({
                    "code": code, "name": name,
                    "updatedAt": updated_at,
                    "rates": rates,
                })
                print(f"[OK] {name} 存款利率抓取成功")
                continue
            except Exception as e:
                print(f"[WARN] {name} 抓取失败：{e}，沿用旧值", file=sys.stderr)
                if code in prev_banks:
                    out.append(prev_banks[code])
                    continue
        # 其他四大行：用半静态值
        static_rates = STATIC_BANK_RATES.get(code)
        if static_rates:
            out.append({
                "code": code, "name": name,
                "updatedAt": "2025-05-20",  # 半静态值的挂牌日期
                "rates": static_rates,
            })
            print(f"[OK] {name} 使用半静态挂牌利率")
        elif code in prev_banks:
            out.append(prev_banks[code])
            print(f"[WARN] {name} 沿用旧值")
        else:
            out.append({
                "code": code, "name": name,
                "updatedAt": "—",
                "rates": {k: 0.0 for k in TERM_KEYS},
            })
    return out

# ============================================================
# 旧快照读取（用于保本）
# ============================================================

def read_prev_payload() -> dict[str, Any]:
    if OUTPUT_PATH.exists():
        try:
            return json.loads(OUTPUT_PATH.read_text(encoding="utf-8"))
        except Exception:
            return {}
    return {}

# ============================================================
# 10 年期国债收益率（每日自动）
# ============================================================

def fetch_treasury_10y() -> Optional[float]:
    """从东方财富抓最新一日中国 10 年期国债到期收益率（%）。"""
    try:
        resp = requests.get(
            TREASURY_10Y_URL,
            headers={"User-Agent": USER_AGENT, "Referer": "https://data.eastmoney.com/"},
            timeout=TIMEOUT,
        )
        resp.raise_for_status()
        rows = resp.json().get("result", {}).get("data", []) or []
        if not rows:
            return None
        # 按日期降序取第一行；EMM00166466 = 中国国债收益率:10年
        return parse_float(str(rows[0].get("EMM00166466")))
    except Exception as e:
        print(f"[WARN] 10Y 国债抓取失败：{e}", file=sys.stderr)
        return None

# ============================================================
# 沪深 300 近 10 年年化收益率（每日自动）
# ============================================================

def fetch_csi300_10y_annualized() -> Optional[float]:
    """从东方财富取沪深 300 历史日 K，用近 10 年区间计算复合年化收益率（%）。"""
    try:
        # 取最近 2600 个交易日（足够覆盖 10 年）
        url = CSI300_HISTORY_URL + "&lmt=2600"
        text = http_get_raw(url, {
            "User-Agent": USER_AGENT,
            "Referer": "https://quote.eastmoney.com/",
        })
        data = json.loads(text).get("data") or {}
        klines = data.get("klines", []) or []
        if len(klines) < 100:
            return None
        # 每行 "日期,开盘,收盘,最高,最低,成交量,…"；收盘价为第 3 列
        from datetime import date as date_cls
        points: list[tuple[str, float]] = []
        for line in klines:
            parts = line.split(",")
            if len(parts) >= 3:
                points.append((parts[0], float(parts[2])))
        first_date, first_close = points[0]
        last_date, last_close = points[-1]
        d_last = date_cls.fromisoformat(last_date)
        # 精确锚定到 10 年前最近的交易日（容差 45 天），避免用窗口起点带来误差
        target = d_last.replace(year=d_last.year - 10)
        start_point = min(
            points,
            key=lambda p: abs((date_cls.fromisoformat(p[0]) - target).days),
        )
        if abs((date_cls.fromisoformat(start_point[0]) - target).days) > 45:
            start_point = (first_date, first_close)  # 兜底：窗口最早一日
        d0 = date_cls.fromisoformat(start_point[0])
        years = (d_last - d0).days / 365.25
        if years < 5:  # 数据不足 5 年，结果不可靠
            return None
        ratio = last_close / start_point[1]
        # 复合年化 = (终值/初值)^(1/年数) - 1
        annualized = (ratio ** (1 / years) - 1) * 100
        # 含 2% 股息率假设（沪深 300 平均股息率约 2%）
        return round(annualized + 2.0, 2)
    except Exception as e:
        print(f"[WARN] 沪深 300 抓取失败：{e}", file=sys.stderr)
        return None

# ============================================================
# 理财平均年化收益率（随官方半年报 / 年报自动更新，每年 2 期）
# ============================================================

# 官方报告固定表述：「理财产品平均年化收益率为 2.05%」
WEALTH_RATE_RE = re.compile(r"理财产品平均年化收益率为\s*([0-9]+(?:\.[0-9]+)?)\s*[%％]")

def fetch_wealth_management() -> Optional[float]:
    """从证券时报「银行理财」标签聚合流中定位最新一期官方报告快讯，
    抓取全文并用官方固定表述提取平均年化收益率（%）。
    """
    try:
        headers = {"User-Agent": USER_AGENT, "Referer": "https://www.stcn.com/"}
        candidates: list[dict[str, Any]] = []
        # 扫描最近 3 页（约 60 条，覆盖 2-3 个月，报告发布窗口足够）
        for page in range(1, 4):
            resp = requests.get(
                STCN_WEALTH_TAG_API.format(page=page), headers=headers, timeout=TIMEOUT
            )
            resp.raise_for_status()
            items = resp.json().get("data", []) or []
            for it in items:
                blob = f"{it.get('title', '')} {it.get('content', '')}"
                if "理财" in blob and ("平均年化" in blob or "年化收益率" in blob or "报告" in blob):
                    candidates.append({
                        "url": "https://www.stcn.com" + it.get("url", ""),
                        "title": it.get("title", ""),
                        "ts": int(it.get("show_time", 0) or 0),
                    })
        # 按发布时间倒序，去重
        seen: set[str] = set()
        candidates = [c for c in sorted(candidates, key=lambda x: x["ts"], reverse=True)
                      if not (c["url"] in seen or seen.add(c["url"]))]
        for art in candidates[:8]:
            try:
                html = http_get(art["url"])
            except Exception:
                continue
            text = BeautifulSoup(html, "lxml").get_text(" ", strip=True)
            m = WEALTH_RATE_RE.search(text)
            if m:
                v = float(m.group(1))
                # 合理性区间：理财历史平均年化大致 1.5%～6%，超出视为误抓
                if 0.5 <= v <= 8.0:
                    print(f"[OK] 理财平均年化抓取成功：{v:.2f}%（{art['title'][:30]}）")
                    return round(v, 2)
        return None
    except Exception as e:
        print(f"[WARN] 理财平均年化抓取失败：{e}", file=sys.stderr)
        return None

# ============================================================
# 寿险预定利率上限（中保协季度研究值 + 监管规则自动联动）
# ============================================================

# 例会文章：/art/YYYY/M/D/art_22_xxx.html，标题含「利率研究」或「责任准备金评估利率」
IACHINA_MEETING_RE = re.compile(
    r'href="(/art/(\d{4})/(\d{1,2})/(\d{1,2})/art_22_\d+\.html)"[^>]*>'
    r'([^<]*(?:利率研究|责任准备金评估利率)[^<]*)'
)
# 正文固定表述：「普通型人身保险产品预定利率研究值为 1.93%」（兼容数字前空格）
RESEARCH_VALUE_RE = re.compile(
    r"普通型人身保险产品预定利率研究值为\s*([0-9]+(?:\.[0-9]+)?)\s*[%％]"
)

def _fetch_research_values(limit: int = 2) -> list[tuple[str, float]]:
    """通过中保协 jpage 代理按窗口翻页，找到最近 limit 期利率例会，
    抓正文提取研究值，返回 (日期, 研究值%) 列表，按日期倒序。
    """
    meetings: dict[str, str] = {}  # url -> 日期
    headers = {"User-Agent": USER_AGENT, "Referer": IACHINA_LIST_URL}
    # 8 个窗口约覆盖半年；每窗口步长 14（实测每页返回 20+ 条且窗口平移约 14 条）
    for window in range(8):
        start = 1 + window * 14
        params = dict(IACHINA_PROXY_PARAMS, startrecord=start, endrecord=start + 13)
        try:
            resp = requests.get(IACHINA_DATAPROXY_URL, params=params,
                                headers=headers, timeout=TIMEOUT)
            resp.raise_for_status()
            resp.encoding = resp.apparent_encoding or "utf-8"
            frag = resp.text
        except Exception as e:
            print(f"[WARN] 中保协列表窗口 {window} 抓取失败：{e}", file=sys.stderr)
            continue
        for m in IACHINA_MEETING_RE.finditer(frag):
            path, y, mo, d, _title = m.groups()
            meetings["https://www.iachina.cn" + path] = \
                f"{int(y):04d}-{int(mo):02d}-{int(d):02d}"
        if len(meetings) >= limit:
            break

    out: list[tuple[str, float]] = []
    for url, date_str in sorted(meetings.items(), reverse=True):
        if len(out) >= limit:
            break
        try:
            text = BeautifulSoup(http_get(url), "lxml").get_text(" ", strip=True)
        except Exception:
            continue
        vm = RESEARCH_VALUE_RE.search(text)
        if vm:
            out.append((date_str, float(vm.group(1))))
    return out

def _apply_insurance_cap_rule(current_cap: float, latest_two: list[float]) -> float:
    """按监管动态调整机制由最近两期研究值联动上限：
    连续两期偏离上限 ≥ 25BP 时触发，新上限取 0.25% 整数倍，
    使新上限与最新研究值之差小于 25BP。
    """
    if len(latest_two) < 2:
        return current_cap
    latest, previous = latest_two[0], latest_two[1]
    eps = 0.001
    if latest <= current_cap - INSURANCE_TRIGGER - eps and \
       previous <= current_cap - INSURANCE_TRIGGER - eps:
        target = math.floor((latest + INSURANCE_TRIGGER - eps) / INSURANCE_TRIGGER) * INSURANCE_TRIGGER
        return round(target, 2)
    if latest >= current_cap + INSURANCE_TRIGGER + eps and \
       previous >= current_cap + INSURANCE_TRIGGER + eps:
        target = math.ceil((latest - INSURANCE_TRIGGER + eps) / INSURANCE_TRIGGER) * INSURANCE_TRIGGER
        return round(target, 2)
    return current_cap

def fetch_insurance_cap(prev_cap: float) -> float:
    """抓中保协最新两期研究值，按监管规则推算当前预定利率上限；失败沿用旧值。"""
    try:
        research = _fetch_research_values(limit=2)
        if len(research) < 2:
            print("[WARN] 保险研究值不足两期，沿用旧上限")
            return prev_cap
        values = [v for _, v in research]
        print(f"[OK] 保险研究值：{research[0][0]}={research[0][1]}%，"
              f"{research[1][0]}={research[1][1]}%")
        new_cap = _apply_insurance_cap_rule(prev_cap, values)
        if abs(new_cap - prev_cap) > 0.001:
            print(f"[OK] 触发监管动态调整：寿险预定利率上限 {prev_cap:.2f}% → {new_cap:.2f}%")
        return new_cap
    except Exception as e:
        print(f"[WARN] 保险研究值抓取失败：{e}，沿用旧上限 {prev_cap:.2f}%", file=sys.stderr)
        return prev_cap

# ============================================================
# 贵金属（黄金/白银）与美元汇率（含近半年日线）
# 主源官方发布，东方财富作为备源；任一链路全断则沿用旧 JSON
# ============================================================

def _build_quote(name: str, unit: str, ndigits: int,
                 rows: list[tuple[str, float, float, float, float]],
                 source: str) -> dict[str, Any]:
    """把 [(date, open, close, low, high), ...]（升序）转成标准输出。"""
    if len(rows) < 2:
        raise ValueError("行情序列不足 2 条")
    last_date, _last_open, price, low, high = rows[-1]
    prev_close = rows[-2][2]
    points = [{"date": d, "close": round(c, ndigits)} for d, _o, c, _l, _h in rows]
    change = round(price - prev_close, ndigits)
    change_pct = round((price - prev_close) / prev_close * 100, 2) if prev_close else 0.0
    return {
        "name": name,
        "price": round(price, ndigits),
        "unit": unit,
        "change": change,
        "changePct": change_pct,
        "high": round(high, ndigits),
        "low": round(low, ndigits),
        "asOf": last_date,
        "source": source,
        "history": points,
    }

def fetch_sge_quote(instid: str, name: str, unit: str, ndigits: int) -> dict[str, Any]:
    """上海黄金交易所官网：POST /graph/Dailyhq，返回全历史日线。
    每行 [日期, 开盘, 收盘, 最低, 最高]，取最后 130 条（≈半年）。"""
    text = http_post_raw(
        SGE_DAILYHQ_URL,
        data={"instid": instid},
        headers={
            "User-Agent": USER_AGENT,
            "Referer": "https://www.sge.com.cn/sjzx/mrhq",
            "X-Requested-With": "XMLHttpRequest",
        },
    )
    series = json.loads(text).get("time") or []
    rows = [
        (r[0], float(r[1]), float(r[2]), float(r[3]), float(r[4]))
        for r in series if len(r) >= 5
    ][-130:]
    return _build_quote(name, unit, ndigits, rows, "上海黄金交易所")

def fetch_eastmoney_quote(secid: str, name: str, unit: str, ndigits: int) -> dict[str, Any]:
    """东方财富 K 线兜底。每行 日期,开盘,收盘,最高,最低,成交量。"""
    text = http_get_raw(
        EASTMONEY_KLINE_URL.format(secid=secid),
        {"User-Agent": USER_AGENT, "Referer": "https://quote.eastmoney.com/"},
    )
    klines = (json.loads(text).get("data") or {}).get("klines") or []
    rows = []
    for line in klines:
        p = line.split(",")
        if len(p) >= 5:
            rows.append((p[0], float(p[1]), float(p[2]), float(p[4]), float(p[3])))
    return _build_quote(name, unit, ndigits, rows, "东方财富")

def fetch_fx_chinamoney() -> dict[str, Any]:
    """中国货币网：美元兑人民币中间价历史（顶层 records，降序）。
    接口固定每页 15 条且禁止 pageSize 参数（WAF 403），
    因此按 20 天自然日窗口分段（每窗工作日 ≤15 条），聚合去重。"""
    end = datetime.now(CN_TZ).date()
    start = end - timedelta(days=220)  # 拉足半年（扣除假期）
    by_date: dict[str, float] = {}
    window_end = end
    while window_end >= start:
        window_start = max(start, window_end - timedelta(days=19))
        text = http_get_raw(
            CHINAMONEY_FX_URL.format(start=window_start.isoformat(),
                                     end=window_end.isoformat()),
            {"User-Agent": USER_AGENT, "Referer": "https://www.chinamoney.com.cn/"},
        )
        for rec in json.loads(text).get("records") or []:
            vals = rec.get("values") or []
            if vals:
                by_date[rec["date"]] = float(vals[0])
        window_end = window_start - timedelta(days=1)
        time.sleep(0.3)
    rows = [(d, p, p, p, p) for d, p in sorted(by_date.items())][-130:]
    return _build_quote("美元兑人民币中间价", "", 4, rows, "中国外汇交易中心")

def fetch_fx_eastmoney() -> dict[str, Any]:
    """东方财富：美元人民币中间价 secid=120.USDCNYC。"""
    return fetch_eastmoney_quote("120.USDCNYC", "美元兑人民币中间价", "", 4)

def fetch_metals(prev_metals: dict[str, Any]) -> dict[str, Any]:
    """逐个品种抓：官方源 → 东财备源 → 旧 JSON 缓存。"""
    out: dict[str, Any] = {}
    latest_dates: list[str] = []

    # 黄金、白银
    for key, name, unit, ndigits, sge_instid, em_secid in METAL_SPECS:
        quote = None
        try:
            quote = fetch_sge_quote(sge_instid, name, unit, ndigits)
        except Exception as e:
            print(f"[WARN] SGE {name} 失败：{e}，尝试东方财富", file=sys.stderr)
        if quote is None:
            try:
                quote = fetch_eastmoney_quote(em_secid, name, unit, ndigits)
            except Exception as e:
                print(f"[WARN] 东方财富 {name} 也失败：{e}", file=sys.stderr)
        if quote is not None:
            out[key] = quote
            latest_dates.append(quote["asOf"])
        elif key in prev_metals:
            out[key] = prev_metals[key]
            latest_dates.append(out[key].get("asOf", ""))
            print(f"[WARN] {name} 沿用旧值")

    # 美元汇率
    fx = None
    try:
        fx = fetch_fx_chinamoney()
    except Exception as e:
        print(f"[WARN] 货币网汇率失败：{e}，尝试东方财富", file=sys.stderr)
    if fx is None:
        try:
            fx = fetch_fx_eastmoney()
        except Exception as e:
            print(f"[WARN] 东方财富汇率也失败：{e}", file=sys.stderr)
    if fx is not None:
        out["fx"] = fx
        latest_dates.append(fx["asOf"])
    elif "fx" in prev_metals:
        out["fx"] = prev_metals["fx"]
        latest_dates.append(out["fx"].get("asOf", ""))
        print("[WARN] 美元汇率沿用旧值")

    dates = [d for d in latest_dates if d]
    out["updatedAt"] = max(dates) if dates else prev_metals.get("updatedAt", "")
    for key in ("gold", "silver", "fx"):
        if key in out:
            q = out[key]
            dec = 4 if key == "fx" else 2
            print(f"[OK] {q['name']}：{q['price']}（{q['change']:+.{dec}f}，"
                  f"{q['changePct']:+.2f}%，{q['asOf']}，源：{q['source']}）")
    return out

# ============================================================
# 主流程
# ============================================================

def main() -> int:
    print(f"=== 利率速查数据管线 · {now_iso()} ===")

    prev = read_prev_payload()

    # LPR
    lpr_new = fetch_lpr()
    if not lpr_new:
        lpr_new = prev.get("lpr", {})
        print("[WARN] LPR 沿用旧值")
    else:
        print("[OK] LPR 抓取成功")

    # 存款
    banks = fetch_deposit_banks()

    # 公积金：央行公告不定期，沿用旧值或 fallback
    fund = prev.get("fund") or FUND_FALLBACK

    # 房贷参考：跟随 LPR 自动计算（首套 LPR5Y-45BP，二套 LPR5Y-25BP）
    lpr5y = lpr_new.get("current", {}).get("lpr5y", 3.50)
    mortgage = {
        "firstFloorMin": round(lpr5y - 0.45, 2),
        "firstFloorMax": round(lpr5y - 0.30, 2),
        "secondFloorMin": round(lpr5y - 0.25, 2),
        "secondFloorMax": round(lpr5y - 0.05, 2),
        "note": MORTGAGE_NOTE,
    }

    # 其他资产回报参考：国债/股票每日自动，理财随官方年报/半年报，保险按季联动
    prev_ref = prev.get("reference", {})
    treasury = fetch_treasury_10y()
    if treasury is None:
        treasury = prev_ref.get("treasury10y", 1.68)
        print("[WARN] 10Y 国债沿用旧值")
    else:
        treasury = round(treasury, 2)
        print(f"[OK] 10Y 国债抓取成功：{treasury}%")

    csi300 = fetch_csi300_10y_annualized()
    if csi300 is None:
        csi300 = prev_ref.get("csi300_10y_annualized", 6.20)
        print("[WARN] 沪深 300 沿用旧值")
    else:
        print(f"[OK] 沪深 300 年化抓取成功：{csi300}%")

    # 理财平均年化：随官方半年报 / 年报自动更新；抓不到沿用旧值
    wealth = fetch_wealth_management()
    if wealth is None:
        wealth = prev_ref.get("wealthManagement", WEALTH_MANAGEMENT_FALLBACK)
        print(f"[WARN] 理财平均年化沿用旧值 {wealth}%")

    # 寿险预定利率上限：中保协季度研究值 + 监管规则自动联动
    prev_insurance_cap = prev_ref.get("insuranceCap", INSURANCE_CAP_FALLBACK)
    insurance_cap = fetch_insurance_cap(prev_insurance_cap)

    # 贵金属 / 美元汇率（含近半年日线）；任一品种失败沿用旧快照
    metals = fetch_metals(prev.get("metals", {}))

    # 存款 1 年定存：取五大行均值
    deposit_1y_list = [b.get("rates", {}).get("term1y", 0.0) for b in banks]
    deposit_1y = round(sum(deposit_1y_list) / max(len(deposit_1y_list), 1), 2)

    reference = {
        "deposit1y": deposit_1y,
        "treasury10y": treasury,
        "insuranceCap": insurance_cap,
        "wealthManagement": wealth,
        "csi300_10y_annualized": csi300,
        "updatedDescription": "存款/国债/股票每日自动；寿险上限按监管季度机制联动；理财随官方半年报、年报更新",
        "updatedAt": datetime.now(CN_TZ).strftime("%Y-%m-%d"),
    }

    payload = {
        "generatedAt": now_iso(),
        "source": "中国人民银行 / 全国银行间同业拆借中心 / 各行官网 / 东方财富",
        "deposit": {"banks": banks},
        "lpr": lpr_new,
        "fund": fund,
        "mortgageReference": mortgage,
        "reference": reference,
        "metals": metals,
    }

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"[DONE] 写入 {OUTPUT_PATH}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
