package com.dinghong.controller.live;

import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/live")
@CrossOrigin(origins = "*")
public class LiveStatusController {

    private final DataSource dataSource;

    public LiveStatusController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/status")
    public Map<String, Object> status(@RequestParam String key) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (key == null || key.trim().isEmpty()) {
            result.put("online", false);
            result.put("status", "INVALID");
            result.put("message", "直播链接无效，请从公众号菜单重新进入");
            return result;
        }

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id,league_name,home_team,away_team,live_status,stream_key,stream_url,stream_type,show_in_wechat " +
                            "FROM match_live WHERE stream_key=? LIMIT 1"
            );
            ps.setString(1, key.trim());

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                result.put("online", false);
                result.put("status", "NOT_FOUND");
                result.put("message", "直播链接无效或本场直播已结束，请从公众号菜单重新进入");
                return result;
            }

            long id = rs.getLong("id");
            String league = safe(rs.getString("league_name"));
            String home = safe(rs.getString("home_team"));
            String away = safe(rs.getString("away_team"));
            String status = safe(rs.getString("live_status"));
            String streamUrl = safe(rs.getString("stream_url"));
            String streamType = safe(rs.getString("stream_type"));
            int show = rs.getInt("show_in_wechat");

            String title = buildTitle(home, away, league);

            boolean online = show != 0
                    && !empty(streamUrl)
                    && "AVAILABLE".equalsIgnoreCase(status);

            result.put("online", online);
            result.put("id", id);
            result.put("key", key.trim());
            result.put("title", title);
            result.put("status", status);
            result.put("streamType", inferType(streamType, streamUrl));

            if (online) {
                result.put("streamUrl", streamUrl);
                result.put("message", "直播已开放，正在为你加载");
            } else {
                result.put("message", userMessage(status, streamUrl, show));
            }

            return result;

        } catch (Exception e) {
            result.put("online", false);
            result.put("status", "ERROR");
            result.put("message", "直播状态暂时无法获取，请稍后刷新");
            return result;
        }
    }

    private String userMessage(String status, String streamUrl, int show) {
        String s = safe(status).toUpperCase();

        if (show == 0) return "本场直播暂未开放，请稍后查看";
        if ("WAITING".equals(s)) return "比赛尚未开始，开赛前请回来刷新";
        if ("NO_RIGHTS".equals(s)) return "当前暂无本场直播信号";
        if (isOfflineStatus(s)) return "本场直播已结束，感谢观看";
        if (empty(streamUrl)) return "直播信号正在接入，请稍后刷新";

        return "本场直播暂不可用，请稍后刷新";
    }

    private boolean isOfflineStatus(String status) {
        if (status == null) return false;

        String s = status.trim().toUpperCase();

        return s.equals("FINISHED")
                || s.equals("OFFLINE")
                || s.equals("ENDED")
                || s.equals("CLOSED")
                || s.equals("DELETE")
                || s.equals("DELETED");
    }

    private String inferType(String streamType, String url) {
        if (!empty(streamType) && !"auto".equalsIgnoreCase(streamType)) {
            return streamType.trim().toLowerCase();
        }

        String u = safe(url).toLowerCase();

        if (u.contains(".m3u8")) return "hls";
        if (u.contains(".flv")) return "flv";

        return "flv";
    }

    private String buildTitle(String home, String away, String league) {
        if (!empty(home) && !empty(away)) {
            return home + " VS " + away;
        }
        if (!empty(home)) return home;
        if (!empty(away)) return away;
        if (!empty(league)) return league;
        return "顶红体育直播";
    }

    private boolean empty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
