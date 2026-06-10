package com.dinghong.controller.admin;

import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/admin/matches")
public class MatchAdminController {

    private final DataSource dataSource;

    public MatchAdminController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public List<Map<String, Object>> list() throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM match_live ORDER BY id DESC");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(toMap(rs));
            }
        }

        return list;
    }

    @PostMapping
    public String add(@RequestBody Map<String, String> body) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO match_live " +
                    "(league_name, home_team, away_team, match_time, live_status, qrcode_url, keywords, stream_key, stream_url, stream_type, show_in_wechat, stream_updated_at) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, body.get("league"));
            ps.setString(2, body.get("home"));
            ps.setString(3, body.get("away"));
            ps.setString(4, body.get("time"));
            ps.setString(5, defaultText(body.get("status"), "WAITING"));
            ps.setString(6, body.get("qr"));
            ps.setString(7, body.get("keywords"));
            ps.setString(8, empty(body.get("streamKey")) ? null : body.get("streamKey"));
            ps.setString(9, body.get("streamUrl"));
            ps.setString(10, defaultText(body.get("streamType"), "auto"));
            ps.setInt(11, boolInt(body.get("showInWechat"), 1));

            if (empty(body.get("streamUrl"))) {
                ps.setNull(12, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(12, new Timestamp(System.currentTimeMillis()));
            }

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next() && empty(body.get("streamKey"))) {
                long id = keys.getLong(1);
                PreparedStatement ups = conn.prepareStatement("UPDATE match_live SET stream_key=? WHERE id=?");
                ups.setString(1, "live_" + id);
                ups.setLong(2, id);
                ups.executeUpdate();
            }
        }

        return "ok";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @RequestBody Map<String, String> body) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String oldStreamUrl = "";
            PreparedStatement query = conn.prepareStatement("SELECT stream_url FROM match_live WHERE id=? LIMIT 1");
            query.setLong(1, id);
            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                oldStreamUrl = safe(rs.getString("stream_url"));
            }

            String newStreamUrl = body.get("streamUrl");
            boolean streamChanged = !Objects.equals(oldStreamUrl, safe(newStreamUrl));

            String sql = "UPDATE match_live SET " +
                    "league_name=?, home_team=?, away_team=?, match_time=?, live_status=?, qrcode_url=?, keywords=?, " +
                    "stream_key=?, stream_url=?, stream_type=?, show_in_wechat=?, " +
                    "stream_updated_at=CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE stream_updated_at END " +
                    "WHERE id=?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, body.get("league"));
            ps.setString(2, body.get("home"));
            ps.setString(3, body.get("away"));
            ps.setString(4, body.get("time"));
            ps.setString(5, defaultText(body.get("status"), "WAITING"));
            ps.setString(6, body.get("qr"));
            ps.setString(7, body.get("keywords"));

            String streamKey = body.get("streamKey");
            if (empty(streamKey)) streamKey = "live_" + id;

            ps.setString(8, streamKey);
            ps.setString(9, newStreamUrl);
            ps.setString(10, defaultText(body.get("streamType"), "auto"));
            ps.setInt(11, boolInt(body.get("showInWechat"), 1));
            ps.setBoolean(12, streamChanged);
            ps.setLong(13, id);

            ps.executeUpdate();
        }

        return "ok";
    }

    @PutMapping("/{id}/stream")
    public String updateStream(@PathVariable Long id, @RequestBody Map<String, String> body) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "UPDATE match_live SET " +
                    "stream_key=IF(stream_key IS NULL OR stream_key='', ?, stream_key), " +
                    "stream_url=?, stream_type=?, live_status=?, show_in_wechat=?, stream_updated_at=CURRENT_TIMESTAMP " +
                    "WHERE id=?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "live_" + id);
            ps.setString(2, body.get("streamUrl"));
            ps.setString(3, defaultText(body.get("streamType"), "auto"));
            ps.setString(4, defaultText(body.get("status"), "AVAILABLE"));
            ps.setInt(5, boolInt(body.get("showInWechat"), 1));
            ps.setLong(6, id);
            ps.executeUpdate();
        }

        return "ok";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM match_live WHERE id=?");
            ps.setLong(1, id);
            ps.executeUpdate();
        }

        return "ok";
    }

    private Map<String, Object> toMap(ResultSet rs) throws Exception {
        Map<String, Object> map = new HashMap<>();

        long id = rs.getLong("id");
        String streamKey = safe(rs.getString("stream_key"));
        if (empty(streamKey)) streamKey = "live_" + id;

        map.put("id", id);
        map.put("league", rs.getString("league_name"));
        map.put("home", rs.getString("home_team"));
        map.put("away", rs.getString("away_team"));
        map.put("time", rs.getString("match_time"));
        map.put("status", rs.getString("live_status"));
        map.put("qr", rs.getString("qrcode_url"));
        map.put("keywords", rs.getString("keywords"));

        map.put("streamKey", streamKey);
        map.put("streamUrl", rs.getString("stream_url"));
        map.put("streamType", rs.getString("stream_type"));
        map.put("showInWechat", rs.getInt("show_in_wechat"));
        map.put("streamUpdatedAt", rs.getString("stream_updated_at"));
        map.put("playUrl", "https://live.5q.lol/play.html?key=" + streamKey);

        return map;
    }

    private String defaultText(String value, String def) {
        return empty(value) ? def : value;
    }

    private int boolInt(String value, int def) {
        if (empty(value)) return def;
        String v = value.trim().toLowerCase();
        if ("0".equals(v) || "false".equals(v) || "no".equals(v) || "off".equals(v)) return 0;
        return 1;
    }

    private boolean empty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
