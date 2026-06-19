package com.dinghong.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class ArticleDraftRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ArticleDraftRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertGenerated(long matchId, String dataJson) {
        String sql = "INSERT INTO article_draft (match_id, data_json, status) VALUES (:matchId, :data, 'GENERATED')";
        Map<String, Object> params = new HashMap<>();
        params.put("matchId", matchId);
        params.put("data", dataJson);
        jdbc.update(sql, params);
    }

    public boolean existsGeneratedForMatch(long matchId) {
        String sql = "SELECT COUNT(*) FROM article_draft WHERE match_id = :matchId AND status IN ('GENERATED','PUBLISHED','REVISED')";
        Map<String, Object> p = Map.of("matchId", matchId);
        Integer cnt = jdbc.queryForObject(sql, p, Integer.class);
        return cnt != null && cnt > 0;
    }
}