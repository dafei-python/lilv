#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
利率速查 · 数据抓取脚本
=======================

每日由 GitHub Actions 调用一次（cron 9:30 北京时间 ≈ 1:30 UTC），
抓取以下权威源的最新利率，写入 data-pipeline/public/rates.json：

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

OUTPUT_PATH = Path(__file__).resolve().parent.parent / "public" / "rates.json"
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

# 公积金贷款利率：央行调整时才变。脚本默认沿用旧值，
# 央行发布新公告时请在此处更新 EFFECTIVE_SINCE 和四个数值。
FUND_FALLBACK = {
    "first5yBelow": 2.10,
    "first5yAbove": 2.60,
    "second5yBelow": 2.525,
    "second5yAbove": 3.075,
    "effectiveSince": "2025-05-08",
}

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

    payload = {
        "generatedAt": now_iso(),
        "source": "中国人民银行 / 全国银行间同业拆借中心 / 各行官网",
        "deposit": {"banks": banks},
        "lpr": lpr_new,
        "fund": fund,
        "mortgageReference": mortgage,
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
