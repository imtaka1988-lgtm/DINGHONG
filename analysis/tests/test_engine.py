import json
from analysis.analysis_engine import analyse_match


def test_analyse_match_dummy(monkeypatch):
    # monkeypatch db helpers to return deterministic fake data
    from analysis import analysis_engine as eng

    class DummyCur:
        def execute(self, *_):
            pass
        def fetchone(self):
            # home eff 120, away eff 110 etc.
            return (120, 110, 105, 100, 3, 2, 'TeamA', 'TeamB', 'Soccer Super League', '2026')
        def fetchall(self):
            return []
    class DummyConn:
        def cursor(self):
            return DummyCur()
        def close(self):
            pass
    monkeypatch.setattr(eng, "_get_db_conn", lambda: DummyConn())

    res = analyse_match(1)
    assert res is not None
    d = json.dumps(res.to_dict())
    assert 'TeamA' in d