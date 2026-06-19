package com.dinghong.repository;

import com.dinghong.entity.MatchStats;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MatchStatsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public MatchStatsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MatchStats> findByMatchId(long matchId) {
        String sql = "SELECT * FROM match_stats WHERE match_id = :matchId";
        Map<String, Object> params = new HashMap<>();
        params.put("matchId", matchId);
        return jdbc.query(sql, params, new MatchStatsRowMapper());
    }

    private static class MatchStatsRowMapper implements RowMapper<MatchStats> {
        @Override
        public MatchStats mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MatchStats(
                    rs.getLong("id"),
                    rs.getLong("match_id"),
                    rs.getString("league"),
                    rs.getString("season"),
                    rs.getString("home_team"),
                    rs.getString("away_team"),
                    rs.getDouble("home_rating"),
                    rs.getDouble("away_rating"),
                    rs.getDouble("home_attack_eff"),
                    rs.getDouble("away_attack_eff"),
                    rs.getDouble("home_defense_eff"),
                    rs.getDouble("away_defense_eff"),
                    rs.getInt("home_last5_win"),
                    rs.getInt("away_last5_win")
            );
        }
    }
}