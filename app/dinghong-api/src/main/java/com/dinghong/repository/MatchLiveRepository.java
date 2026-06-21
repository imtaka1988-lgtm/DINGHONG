package com.dinghong.repository;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * match_live 表的数据访问层。
 * 替代 Controller 中直接 DataSource.getConnection() 写 SQL 的模式。
 */
@Repository
public class MatchLiveRepository {

    private final DataSource dataSource;

    public MatchLiveRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 查询单场比赛。
     */
    public MatchLiveRow findById(long id) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, stream_key, home_team, away_team, match_time FROM match_live WHERE id=? LIMIT 1")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MatchLiveRow row = new MatchLiveRow();
                    row.id = rs.getLong("id");
                    row.streamKey = nvl(rs.getString("stream_key"));
                    row.homeTeam = nvl(rs.getString("home_team"));
                    row.awayTeam = nvl(rs.getString("away_team"));
                    row.matchTime = nvl(rs.getString("match_time"));
                    return row;
                }
            }
        }
        return null;
    }

    /**
     * 生成或返回 stream_key。
     */
    public String ensureStreamKey(long id, String current) throws Exception {
        if (current != null && !current.isEmpty()) return current;
        String key = "live_" + id;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE match_live SET stream_key=? WHERE id=?")) {
            ps.setString(1, key);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
        return key;
    }

    /**
     * 更新二维码 URL 和 media_id。
     */
    public void updateQr(long id, String imageUrl, String mediaId) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE match_live SET qrcode_url=?, wechat_media_id=? WHERE id=?")) {
            ps.setString(1, imageUrl);
            ps.setString(2, mediaId);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    public static class MatchLiveRow {
        public long id;
        public String streamKey;
        public String homeTeam;
        public String awayTeam;
        public String matchTime;
    }
}
