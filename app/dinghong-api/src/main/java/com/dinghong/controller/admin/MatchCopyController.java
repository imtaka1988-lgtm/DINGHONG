package com.dinghong.controller.admin;

import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

@RestController
public class MatchCopyController {

    private final DataSource dataSource;

    public MatchCopyController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostMapping("/admin/matches/{id}/copy")
    public String copy(@PathVariable Long id) {
        try (Connection conn = dataSource.getConnection()) {

            String sql = "INSERT INTO match_live " +
                    "(sport_type, league_name, home_team, away_team, match_time, live_status, qrcode_url, keywords, wechat_media_id) " +
                    "SELECT sport_type, league_name, home_team, away_team, match_time, 'WAITING', '', keywords, NULL " +
                    "FROM match_live WHERE id=?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setLong(1, id);
            int rows = ps.executeUpdate();

            return "success:" + rows;

        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
}
