package com.dinghong.repository;

import com.dinghong.entity.Injury;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class InjuryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public InjuryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Injury> findByMatchId(long matchId) {
        String sql = "SELECT * FROM injuries WHERE match_id = :matchId";
        Map<String, Object> params = new HashMap<>();
        params.put("matchId", matchId);
        return jdbc.query(sql, params, new InjuryRowMapper());
    }

    private static class InjuryRowMapper implements RowMapper<Injury> {
        @Override
        public Injury mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Injury(
                    rs.getLong("id"),
                    rs.getLong("match_id"),
                    rs.getString("team"),
                    rs.getString("player_name"),
                    rs.getString("position"),
                    rs.getString("status"),
                    rs.getString("note")
            );
        }
    }
}