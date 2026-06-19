package com.dinghong.repository;

import com.dinghong.entity.MarketMove;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MarketMoveRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public MarketMoveRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MarketMove> findByMatchId(long matchId) {
        String sql = "SELECT * FROM market_moves WHERE match_id = :matchId ORDER BY last_update DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("matchId", matchId);
        return jdbc.query(sql, params, new MarketMoveRowMapper());
    }

    private static class MarketMoveRowMapper implements RowMapper<MarketMove> {
        @Override
        public MarketMove mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp ts = rs.getTimestamp("last_update");
            Instant last = ts != null ? ts.toInstant() : null;
            return new MarketMove(
                    rs.getLong("id"),
                    rs.getLong("match_id"),
                    rs.getString("market"),
                    rs.getString("bookmaker"),
                    (Double) rs.getObject("open_price_home"),
                    (Double) rs.getObject("open_price_away"),
                    (Double) rs.getObject("open_point_home"),
                    (Double) rs.getObject("open_point_away"),
                    (Double) rs.getObject("live_price_home"),
                    (Double) rs.getObject("live_price_away"),
                    (Double) rs.getObject("live_point_home"),
                    (Double) rs.getObject("live_point_away"),
                    last
            );
        }
    }
}