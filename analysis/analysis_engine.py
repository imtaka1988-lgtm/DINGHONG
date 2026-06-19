"""顶红公众号 - 结构化比赛分析引擎 (prototype)

使用方式:
    python -m analysis.analysis_engine 1234  # match_id

输出:
    JSON 字符串, 可直接写入 article_draft.data_json

依赖:
    - mysql-connector-python  (pip install mysql-connector-python)
    - numpy                  (pip install numpy)

目前实现: 轻量简化版 Poisson 进球模型 (足球) / Pace & Rating 线性估计 (篮球)
"""

from __future__ import annotations

import json
import math
import os
import sys
import time
from dataclasses import asdict, dataclass
from typing import Dict, List, Any

import mysql.connector  # type: ignore
import numpy as np  # type: ignore


@dataclass
class TeamRating:
    attack_eff: float  # 进攻效率
    defense_eff: float  # 防守效率
    last5_win: int     # 近 5 场胜场


@dataclass
class MatchModelResult:
    match_id: int
    league: str
    season: str
    home_team: str
    away_team: str
    rating_diff: float
    exp_home: float
    exp_away: float
    spreads_suggested: List[float]
    totals_suggested: List[float]
    key_players_out: List[str]
    confidence: float  # 0-1

    def to_dict(self):
        return asdict(self)


# ---------------------------------------------------------------------------
# DB helpers
# ---------------------------------------------------------------------------

def _get_db_conn():
    conn = mysql.connector.connect(
        host=os.getenv("DB_HOST", "127.0.0.1"),
        port=int(os.getenv("DB_PORT", 3306)),
        user=os.getenv("DB_USER", "dinghong"),
        password=os.getenv("DB_PASSWORD", ""),
        database=os.getenv("DB_NAME", "dinghong"),
    )
    return conn


def _load_stats(cursor, match_id: int) -> Dict[str, TeamRating]:
    cursor.execute("SELECT home_attack_eff, away_attack_eff, home_defense_eff, away_defense_eff, home_last5_win, away_last5_win, home_team, away_team, league, season FROM match_stats WHERE match_id=%s LIMIT 1", (match_id,))
    row = cursor.fetchone()
    if not row:
        return {}
    return {
        "home": TeamRating(row[0] or 0, row[2] or 0, row[4] or 0),
        "away": TeamRating(row[1] or 0, row[3] or 0, row[5] or 0),
        "meta": {
            "home_team": row[6],
            "away_team": row[7],
            "league": row[8],
            "season": row[9],
        }
    }


def _load_injuries(cursor, match_id: int) -> List[str]:
    cursor.execute("SELECT player_name FROM injuries WHERE match_id=%s AND status='OUT'", (match_id,))
    return [r[0] for r in cursor.fetchall()]


# ---------------------------------------------------------------------------
# Model helpers
# ---------------------------------------------------------------------------

def _poisson_expected_goals(att: float, def_eff: float) -> float:
    # 极简线性组合 → λ，限制范围 0.2~3.5
    lam = max(0.2, min(3.5, 0.002 * att + 0.001 * def_eff))
    return lam


def _basketball_expected_points(att: float, def_eff: float) -> float:
    pace = 98  # league average possessions
    off = att * 0.01  # scale to points / possession
    exp = pace * off
    return max(80, min(140, exp))


# ---------------------------------------------------------------------------
# Main analyse function
# ---------------------------------------------------------------------------

def analyse_match(match_id: int) -> MatchModelResult | None:
    conn = _get_db_conn()
    cur = conn.cursor()

    data = _load_stats(cur, match_id)
    if not data:
        return None
    home: TeamRating = data["home"]
    away: TeamRating = data["away"]
    meta = data["meta"]
    injuries = _load_injuries(cur, match_id)

    # 简易类别判断：足球/篮球
    is_football = meta["league"].lower().find("soccer") != -1 or meta["league"].find("足球") != -1

    if is_football:
        lam_home = _poisson_expected_goals(home.attack_eff, away.defense_eff)
        lam_away = _poisson_expected_goals(away.attack_eff, home.defense_eff)
        spreads = [round(lam_home - lam_away + d, 1) for d in (-0.25, 0, 0.25)]
        totals = [round(lam_home + lam_away + d, 1) for d in (-0.5, 0, 0.5)]
    else:
        pts_home = _basketball_expected_points(home.attack_eff, away.defense_eff)
        pts_away = _basketball_expected_points(away.attack_eff, home.defense_eff)
        spreads = [round(pts_home - pts_away + d, 1) for d in (-1.5, 0, 1.5)]
        totals = [round(pts_home + pts_away + d, 1) for d in (-3, 0, 3)]
        lam_home, lam_away = pts_home, pts_away

    rating_diff = round(home.attack_eff - away.attack_eff, 2)

    conf = min(0.9, 0.5 + 0.05 * abs(rating_diff))

    conn.close()
    return MatchModelResult(
        match_id=match_id,
        league=meta["league"],
        season=meta["season"],
        home_team=meta["home_team"],
        away_team=meta["away_team"],
        rating_diff=rating_diff,
        exp_home=round(lam_home, 2),
        exp_away=round(lam_away, 2),
        spreads_suggested=spreads,
        totals_suggested=totals,
        key_players_out=injuries,
        confidence=round(conf, 2),
    )


# ---------------------------------------------------------------------------
# CLI helper
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: python -m analysis.analysis_engine <match_id>")
        sys.exit(1)
    mid = int(sys.argv[1])
    t0 = time.time()
    res = analyse_match(mid)
    if res is None:
        print(json.dumps({"ok": False, "message": "no stats"}, ensure_ascii=False))
        sys.exit(0)
    print(json.dumps(res.to_dict(), ensure_ascii=False))
    dt = (time.time() - t0) * 1000
    print(f"# analysis done in {dt:.1f} ms", file=sys.stderr)
