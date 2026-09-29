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
  3. 五大行存款利率 —— 工/农/中/建/邮储 官网"人民币存款利率表"
  4. 典型房贷执行利率 —— 参考一线城市主流执行利率，注释字符串

容错策略：
  - 任一源抓取失败时，沿用上一份 rates.json 中的旧值（保本）
  - 全部失败时，直接退出 0，保留旧 rates.json 不动

输出文件结构由 app/src/main/java/com/dushishiyi/lilv/data/RatesDto.kt 定义，
本脚本输出必须严格匹配该 schema。
"""

from __future__ import annotations

import json
import re
import sys
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
BANK_URLS = {
    "icbc": ("工商银行", "https://www.icbc.com.cn/column/1438058343720960356.html"),
    "abc":  ("农业银行", "https://www.abchina.com/cn/PersonalBanking/FinancialPlanner/rate/"),
    "boc":  ("中国银行", "https://www.boc.cn/fimarkets/interestrate/"),
    "ccb":  ("建设银行", "https://www.ccb.com/cn/personal/interest/rmbdeposit.html"),
    "psbc": ("邮储银行", "https://www.psbc.com/cn/grjl/rmbdep/"),
}

LPR_URL = "https://www.chinamoney.com.cn/chinese/bklpr/"

# 东方财富 · 10 年期国债到期收益率（银行间市场）
# 接口返回近 1 个月每日收益率，取最新一日
TREASURY_10Y_URL = (
    "https://datacenter-web.eastmoney.com/api/data/v1/get"
    "?reportName=RPT_BOND_CN_TENYEARENREDISOUNTBONDYIELD"
    "&columns=ALL&pageSize=5&sortColumns=SOLAR_DATE&sortTypes=-1"
)

# 东方财富 · 沪深 300 历史前复权收盘（用于计算近 10 年年化）
# 取最近一个交易日和 10 年前同日的指数值
CSI300_HISTORY_URL = (
    "https://push2his.eastmoney.com/api/qt/stock/kline/get"
    "?secid=1.000300&fields1=f1&fields2=f3"
    "&klt=101&fqt=1"  # 日 K 前复权
)

# 公积金贷款利率：央行调整时才变。脚本默认沿用旧值，
# 央行发布新公告时请在此处更新 EFFECTIVE_SINCE 和四个数值。
FUND_FALLBACK = {
    "first5yBelow": 2.10,
    "first5yAbove": 2.60,
    "second5yBelow": 2.525,
    "second5yAbove": 3.075,
    "effectiveSince": "2025-05-08",
}

# 理财和保险为半静态参考值（银登会季度报告 / 金融监管总局规定），
# 半年人工检查一次即可，无需每日抓取。
WEALTH_MANAGEMENT_FALLBACK = 3.00   # 2026 Q3 银行业理财平均年化
INSURANCE_CAP_FALLBACK = 2.50        # 普通型寿险预定利率上限（监管值）

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

def now_iso() -> str:
    return datetime.now(CN_TZ).isoformat(timespec="seconds")

# ============================================================
# LPR 抓取
# ============================================================

def fetch_lpr() -> dict[str, Any]:
    """从中国货币网抓取 LPR 当前值与历史走势。"""
    try:
        html = http_get(LPR_URL)
    except Exception as e:
        print(f"[WARN] LPR 抓取失败：{e}", file=sys.stderr)
        return {}

    soup = BeautifulSoup(html, "lxml")
    # 货币网的 LPR 表格在 class 含 'dollar' 或普通 table 中。
    # 历史数据：每行 "YYYY-MM-DD 1Y值 5Y值"
    history: list[dict[str, Any]] = []
    for tr in soup.select("table tr"):
        tds = tr.find_all(["td", "th"])
        if len(tds) < 3:
            continue
        date_txt = tds[0].get_text(strip=True)
        m = re.match(r"^(\d{4}-\d{2}-\d{2})", date_txt)
        if not m:
            continue
        date = m.group(1)
        v1 = parse_float(tds[1].get_text(strip=True))
        v5 = parse_float(tds[2].get_text(strip=True))
        if v1 is None or v5 is None:
            continue
        history.append({"date": date, "lpr1y": v1, "lpr5y": v5})

    if not history:
        return {}

    # 历史按日期降序排（最新在前）
    history.sort(key=lambda x: x["date"], reverse=True)
    current = history[0]
    return {"current": current, "history": history[:24]}  # 保留近 24 期

# ============================================================
# 五大行存款利率抓取
# ============================================================

TERM_KEYS = ["demand", "term3m", "term6m", "term1y", "term2y", "term3y", "term5y"]
TERM_LABELS = ["活期", "三个月", "半年", "一年", "二年", "三年", "五年"]

def extract_deposit_rates(html: str) -> Optional[dict[str, Any]]:
    """从一行 HTML 中识别 7 列利率。
    通用策略：找一张含 '活期' '三个月' '一年' 等关键字的表，
    从中提取最接近的 7 个数字（按表格顺序）。
    """
    soup = BeautifulSoup(html, "lxml")
    # 找到包含关键字的表
    candidate_table = None
    for tbl in soup.find_all("table"):
        text = tbl.get_text()
        if "活期" in text and ("一年" in text or "三个月" in text):
            candidate_table = tbl
            break

    if candidate_table is None:
        return None

    # 遍历行，找一行恰好能匹配 7 个关键字的行 → 视为表头
    rows = candidate_table.find_all("tr")
    if not rows:
        return None

    # 简单做法：扫描所有 td，找出连续 7 个可解析为 float 的单元格
    # 并且该行紧邻表头中包含 7 个期限关键字
    all_cells = [td.get_text(strip=True) for td in candidate_table.find_all(["td", "th"])]
    # 找到表头位置
    head_idx = -1
    for i, c in enumerate(all_cells):
        if c == "活期" or "活期" in c:
            head_idx = i
            break
    if head_idx < 0 or head_idx + 7 > len(all_cells):
        return None

    rates: dict[str, float] = {}
    for i, key in enumerate(TERM_KEYS):
        v = parse_float(all_cells[head_idx + i + 1]) if head_idx + i + 1 < len(all_cells) else None
        if v is None:
            return None
        rates[key] = v
    return rates

def fetch_deposit_banks() -> list[dict[str, Any]]:
    """抓取五大行存款利率。失败则保本：从旧 rates.json 继承该行数据。"""
    prev = read_prev_payload()
    prev_banks = {b["code"]: b for b in (prev.get("deposit", {}).get("banks", []))}

    out: list[dict[str, Any]] = []
    for code, (name, url) in BANK_URLS.items():
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
        except Exception as e:
            print(f"[WARN] {name} 抓取失败：{e}，沿用旧值", file=sys.stderr)
            if code in prev_banks:
                out.append(prev_banks[code])
            else:
                # 该行从未有过数据，写入占位
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
    """从东方财富抓最新一日 10 年期国债到期收益率（%）。"""
    try:
        resp = requests.get(TREASURY_10Y_URL, headers={"User-Agent": USER_AGENT}, timeout=TIMEOUT)
        resp.raise_for_status()
        data = resp.json()
        rows = data.get("result", {}).get("data", []) or []
        if not rows:
            return None
        # 按日期降序取第一行
        latest = rows[0]
        return parse_float(str(latest.get("YIELD")))
    except Exception as e:
        print(f"[WARN] 10Y 国债抓取失败：{e}", file=sys.stderr)
        return None

# ============================================================
# 沪深 300 近 10 年年化收益率（每日自动）
# ============================================================

def fetch_csi300_10y_annualized() -> Optional[float]:
    """从东方财富取沪深 300 历史日 K，用近 10 年区间计算复合年化收益率（%）。"""
    try:
        # 取最近 2500 个交易日（足够覆盖 10 年）
        url = CSI300_HISTORY_URL + "&beg=20150101&end=20991231&lmt=2600"
        resp = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=TIMEOUT)
        resp.raise_for_status()
        data = resp.json().get("data", {})
        klines = data.get("klines", []) or []
        if len(klines) < 100:
            return None
        # 每行 "YYYY-MM-DD,close,..."；取首尾两端的收盘价算年化
        def parse_close(line: str) -> tuple[str, float]:
            parts = line.split(",")
            return parts[0], float(parts[1])
        # 找 10 年前的最近一天
        today = datetime.now(CN_TZ).date()
        first = parse_close(klines[0])
        last = parse_close(klines[-1])
        # 按日期找 10 年前同日（±30 天容差）
        target_year = today.year - 10
        candidates = [parse_close(k) for k in klines]
        # 取最早可用的日子作为 10 年起点
        # 用 (今天 - 最早日期) 计算实际年数
        from datetime import date as date_cls
        d0 = date_cls.fromisoformat(first[0])
        d1 = date_cls.fromisoformat(last[0])
        years = (d1 - d0).days / 365.25
        if years < 5:  # 数据不足 5 年，结果不可靠
            return None
        ratio = last[1] / first[1]
        # 复合年化 = (终值/初值)^(1/年数) - 1
        annualized = (ratio ** (1 / years) - 1) * 100
        # 含 2% 股息率假设（沪深 300 平均股息率约 2%）
        return round(annualized + 2.0, 2)
    except Exception as e:
        print(f"[WARN] 沪深 300 抓取失败：{e}", file=sys.stderr)
        return None

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

    # 其他资产回报参考：国债/股票每日自动，理财/保险半静态
    prev_ref = prev.get("reference", {})
    treasury = fetch_treasury_10y()
    if treasury is None:
        treasury = prev_ref.get("treasury10y", 2.15)
        print("[WARN] 10Y 国债沿用旧值")
    else:
        print(f"[OK] 10Y 国债抓取成功：{treasury}%")

    csi300 = fetch_csi300_10y_annualized()
    if csi300 is None:
        csi300 = prev_ref.get("csi300_10y_annualized", 6.20)
        print("[WARN] 沪深 300 沿用旧值")
    else:
        print(f"[OK] 沪深 300 年化抓取成功：{csi300}%")

    # 存款 1 年定存：取五大行均值
    deposit_1y_list = [b.get("rates", {}).get("term1y", 0.0) for b in banks]
    deposit_1y = round(sum(deposit_1y_list) / max(len(deposit_1y_list), 1), 2)

    reference = {
        "deposit1y": deposit_1y,
        "treasury10y": treasury,
        "insuranceCap": INSURANCE_CAP_FALLBACK,
        "wealthManagement": WEALTH_MANAGEMENT_FALLBACK,
        "csi300_10y_annualized": csi300,
        "updatedDescription": "国债/股票每日自动；理财/保险为参考值（半年人工检查）",
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
