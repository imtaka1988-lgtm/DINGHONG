package com.dinghong.service;

import com.dinghong.service.ai.DeepSeekService;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Service
public class MatchDbService {

    private final DataSource dataSource;
    private final DeepSeekService deepSeekService;

    public MatchDbService(DataSource dataSource, DeepSeekService deepSeekService) {
        this.dataSource = dataSource;
        this.deepSeekService = deepSeekService;
    }
    public String getImageMediaId(String content) {
        if (empty(content)) return null;

        content = content.trim();

        // 1. 用户点“最近直播”后，如果当前只有一场可用直播，直接回复二维码海报
        if ("最近直播".equals(content) || "直播".equals(content) || "看直播".equals(content)) {
            String onlyMediaId = getOnlyRecentLiveMediaId();
            if (!empty(onlyMediaId)) return onlyMediaId;
            return null;
        }

        // 2. 用户回复 1 / 2 / 3 或 直播1 / 直播2，直接返回对应二维码海报
        if (isLiveIndexCommand(content)) {
            String mediaId = getRecentLiveMediaIdByIndex(content);
            if (!empty(mediaId)) return mediaId;
        }

        // 3. 用户直接回复球队名 / 联赛名 / 关键词，直接返回匹配比赛二维码海报
        if (!isMenuCommand(content) && !isLiveIndexCommand(content)) {
            String mediaId = getMatchMediaIdByKeyword(content);
            if (!empty(mediaId)) return mediaId;
        }

        return null;
    }


    private String getMatchMediaIdByKeyword(String keyword) {
        if (empty(keyword)) return "";

        String q = keyword.trim();
        if (q.length() < 2) return "";

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM match_live " +
                    "WHERE show_in_wechat=1 " +
                    "AND live_status='AVAILABLE' " +
                    "AND stream_url IS NOT NULL AND TRIM(stream_url)<>'' " +
                    "AND wechat_media_id IS NOT NULL AND TRIM(wechat_media_id)<>'' " +
                    "AND (league_name LIKE ? OR home_team LIKE ? OR away_team LIKE ? OR keywords LIKE ?) " +
                    "ORDER BY " +
                    "CASE " +
                    "WHEN home_team=? OR away_team=? THEN 0 " +
                    "WHEN keywords LIKE ? THEN 1 " +
                    "WHEN league_name LIKE ? THEN 2 " +
                    "ELSE 3 END, " +
                    "id DESC LIMIT 1"
            );

            String kw = "%" + q + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ps.setString(4, kw);

            ps.setString(5, q);
            ps.setString(6, q);
            ps.setString(7, kw);
            ps.setString(8, kw);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String mediaId = safe(rs.getString("wechat_media_id"));
                if (!empty(mediaId)) {
                    System.out.println("[KEYWORD_QR_MATCH] keyword=" + q
                            + ", match=" + safe(rs.getString("home_team")) + " VS " + safe(rs.getString("away_team"))
                            + ", mediaId=" + mediaId);
                    return mediaId;
                }
            }
        } catch (Exception e) {
            System.out.println("[KEYWORD_QR_MATCH_ERROR] " + e.getMessage());
        }

        return "";
    }

    public String reply(String content) {

        if (empty(content)) return welcome();

        content = content.trim();

        if ("今日足球".equals(content)) return todayBySport("football");
        if ("今日篮球".equals(content)) return todayBySport("basketball");
        if ("今日比赛".equals(content)) return todayAll();
        if ("最近直播".equals(content) || "直播列表".equals(content) || "直播赛事".equals(content)) return recentLiveList();
        if (isLiveIndexCommand(content)) return recentLiveByIndex(content);
        if ("查找比赛".equals(content) || "查直播".equals(content)) return searchHelp();
        if ("直播说明".equals(content)) return liveHelp();

        if ("今日推荐".equals(content)) return latestArticleForPreview();
        if ("昨日复盘".equals(content)) return latestReviewSmart();
        if ("最新文章".equals(content)) return latestArticles(null, 3);
        if ("编辑部".equals(content)) return editorRoom();

        if ("智能客服".equals(content)) return aiWelcome();
        if ("使用说明".equals(content)) return help();
        if ("联系人工".equals(content)) return contact();

        String match = searchMatch(content);
        if (!empty(match)) return match;

        return aiCustomer(content);
    }


    public java.util.List<java.util.Map<String, String>> recentLiveNewsItems() {
        java.util.List<java.util.Map<String, String>> items = new java.util.ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id,league_name,home_team,away_team,match_time,live_status,stream_key,stream_url,qrcode_url " +
                            "FROM match_live " +
                            "WHERE show_in_wechat=1 " +
                            "AND live_status IN ('AVAILABLE','WAITING') " +
                            "ORDER BY " +
                            "CASE " +
                            "WHEN live_status='AVAILABLE' AND stream_url IS NOT NULL AND TRIM(stream_url)<>'' THEN 0 " +
                            "WHEN live_status='WAITING' THEN 1 " +
                            "ELSE 2 END, " +
                            "CASE WHEN match_time IS NULL OR TRIM(match_time)='' THEN 1 ELSE 0 END, " +
                            "match_time ASC, id DESC LIMIT 8"
            );

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                long id = rs.getLong("id");
                String league = safe(rs.getString("league_name"));
                String home = safe(rs.getString("home_team"));
                String away = safe(rs.getString("away_team"));
                String time = safe(rs.getString("match_time"));
                String status = safe(rs.getString("live_status"));
                String streamKey = safe(rs.getString("stream_key"));
                String streamUrl = safe(rs.getString("stream_url"));
                String picUrl = safe(rs.getString("qrcode_url"));

                if (empty(streamKey)) {
                    streamKey = "live_" + id;
                }

                java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
                item.put("title", newsTitle(league, home, away));
                item.put("description", newsDescription(status, streamUrl, time));
                item.put("picUrl", picUrl);
                item.put("url", "https://live.5q.lol/play.html?key=" + streamKey);

                items.add(item);
            }

        } catch (Exception e) {
            System.out.println("[RECENT_LIVE_NEWS_ERROR] " + e.getMessage());
        }

        return items;
    }



    private String newsTitle(String league, String home, String away) {
        String teams = matchTitle(home, away);
        if (!empty(league)) return league + "｜" + teams;
        return "赛事直播｜" + teams;
    }

    private String matchTitle(String home, String away) {
        if (!empty(home) && !empty(away)) return home + " vs " + away;
        if (!empty(home)) return home;
        if (!empty(away)) return away;
        return "顶红体育直播";
    }

    private String newsDescription(String status, String streamUrl, String time) {
        String prefix;
        if ("AVAILABLE".equals(status) && !empty(streamUrl)) {
            prefix = "正在直播";
        } else if ("AVAILABLE".equals(status)) {
            prefix = "信号接入中";
        } else if ("WAITING".equals(status)) {
            prefix = "即将开始";
        } else {
            prefix = "状态更新中";
        }

        StringBuilder sb = new StringBuilder(prefix);
        if (!empty(time)) sb.append("｜").append(time);
        sb.append("｜点击进入直播页");
        return sb.toString();
    }

    private String liveStatusText(String status, String streamUrl) {
        if ("AVAILABLE".equals(status) && !empty(streamUrl)) return "正在直播，可进入观看";
        if ("AVAILABLE".equals(status)) return "直播信号正在接入，请稍后刷新";
        if ("WAITING".equals(status)) return "比赛尚未开始，开赛前请回来刷新";
        if ("NO_RIGHTS".equals(status)) return "当前暂无本场直播信号";
        if ("FINISHED".equals(status)) return "本场直播已结束";
        return "直播状态更新中";
    }




    public java.util.List<java.util.Map<String, String>> articleNewsItemsByMenu(String menuName, int limit) {
        // 强制硬规则：
        // 今日推荐：只查北京时间今天生成/发布到本地库的 PREVIEW，不允许拿昨天文章兜底。
        // 昨日复盘：只查北京时间今天生成/发布到本地库的 REVIEW，因为它是今天复盘昨天预测。
        // 最新文章：先查本地库最新 3 篇；本地没有可用链接时，再兜底微信已发布接口。
        java.util.List<java.util.Map<String, String>> items = new java.util.ArrayList<>();

        if ("今日推荐".equals(menuName) || "昨日复盘".equals(menuName)) {
            items = articleNewsItemsFromDbByMenu(menuName, limit);
            System.out.println("[CONTENT_MENU_HARD] menu=" + menuName + " source=db_today_only count=" + (items == null ? 0 : items.size()));

            if (items != null && !items.isEmpty()) {
                return items;
            }

            return new java.util.ArrayList<>();
        }

        if ("最新文章".equals(menuName)) {
            items = articleNewsItemsFromDbByMenu(menuName, limit);
            System.out.println("[CONTENT_MENU_HARD] menu=" + menuName + " source=db_latest_first count=" + (items == null ? 0 : items.size()));

            if (items != null && !items.isEmpty()) {
                return items;
            }

            // 重要：不再使用微信接口旧数据兜底。
            // 微信已发布文章接口可能延迟或返回旧文章，不能用旧三篇冒充最新文章。
            System.out.println("[CONTENT_MENU_HARD] menu=" + menuName + " source=no_wechat_fallback count=0");
            return new java.util.ArrayList<>();
        }

        return items;
    }

    public String articleMenuEmptyText(String menuName) {
        if ("今日推荐".equals(menuName)) {
            return "今日推荐还在路上。\n\n编辑部正在整理今天的赛前观点，稍后更新后再来查看。";
        }
        if ("昨日复盘".equals(menuName)) {
            return "昨日复盘还在整理中。\n\n编辑部会在赛果确认后，更新昨天预测的命中与偏差分析。";
        }
        if ("最新文章".equals(menuName)) {
            return "暂时没有获取到最新文章。\n\n请稍后再试，或进入公众号历史消息查看。";
        }
        return "内容还在整理中，请稍后再来。";
    }

    private java.util.List<java.util.Map<String, String>> articleNewsItemsFromWechatByMenu(String menuName, int limit) {
        java.util.List<java.util.Map<String, String>> items = new java.util.ArrayList<>();

        try {
            String appId = System.getenv("WECHAT_APPID");
            String secret = System.getenv("WECHAT_SECRET");
            if (empty(appId) || empty(secret)) return items;

            String tokenUrl =
                    "https://api.weixin.qq.com/cgi-bin/token"
                    + "?grant_type=client_credential"
                    + "&appid=" + appId
                    + "&secret=" + secret;

            String tokenJson = readUrl(tokenUrl);
            String token = extractJson(tokenJson, "access_token");
            if (empty(token)) return items;

            String api =
                    "https://api.weixin.qq.com/cgi-bin/freepublish/batchget?access_token="
                    + token;

            String body = "{\"offset\":0,\"count\":20,\"no_content\":1}";
            String json = postJson(api, body);

            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            com.fasterxml.jackson.databind.JsonNode arr = root.get("item");
            if (arr == null || !arr.isArray()) return items;

            String category = articleMenuCategory(menuName);
            java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
            boolean requireToday = articleMenuRequiresToday(menuName);

            for (com.fasterxml.jackson.databind.JsonNode itemNode : arr) {
                long publishEpoch = firstLong(itemNode, "publish_time", "update_time", "create_time");

                if (requireToday && publishEpoch > 0 && !epochInBeijingDate(publishEpoch, today)) {
                    continue;
                }

                // 今日推荐 / 昨日复盘必须能判断发布时间；否则不使用微信结果，避免把旧文冒充今日内容。
                if (requireToday && publishEpoch <= 0) {
                    continue;
                }

                com.fasterxml.jackson.databind.JsonNode content = itemNode.get("content");
                com.fasterxml.jackson.databind.JsonNode news = content == null ? null : content.get("news_item");
                if (news == null || !news.isArray()) continue;

                for (com.fasterxml.jackson.databind.JsonNode n : news) {
                    String title = nodeText(n, "title");
                    String digest = nodeText(n, "digest");
                    String url = nodeText(n, "url");
                    String thumbUrl = nodeText(n, "thumb_url");

                    if (empty(title) || empty(url)) continue;
                    if (!articleCategoryMatch(category, title, digest)) continue;

                    java.util.Map<String, String> card = new java.util.LinkedHashMap<>();
                    card.put("title", articleCardTitle(category, title));
                    card.put("description", empty(digest) ? articleCardDesc(category) : digest);
                    card.put("picUrl", thumbUrl);
                    card.put("url", url.replace("\\/", "/"));
                    items.add(card);

                    if (items.size() >= Math.max(1, limit)) return items;
                }
            }

        } catch (Exception e) {
            System.out.println("[CONTENT_MENU_WECHAT_ERROR] menu=" + menuName + " error=" + e.getMessage());
        }

        return items;
    }

    private java.util.List<java.util.Map<String, String>> articleNewsItemsFromDbByMenu(String menuName, int limit) {
        java.util.List<java.util.Map<String, String>> items = new java.util.ArrayList<>();

        String category = articleMenuCategory(menuName);
        boolean requireToday = articleMenuRequiresToday(menuName);

        try (Connection conn = dataSource.getConnection()) {
            String sql =
                    "SELECT id,title,wechat_title,wechat_summary,article_category,article_url,created_at " +
                    "FROM article_task " +
                    "WHERE status <> 'ARCHIVED' ";

            if ("PREVIEW".equals(category) || "REVIEW".equals(category)) {
                sql += "AND article_category=? ";
            }

            // 今日推荐、昨日复盘都取今天创作/发布的文章。
            // 昨日复盘的业务含义：今天写昨天预测的复盘，不是昨天发布的复盘。
            if (requireToday) {
                sql += "AND DATE(created_at)=CURDATE() ";
            }

            sql += "ORDER BY created_at DESC, id DESC LIMIT ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            int idx = 1;
            if ("PREVIEW".equals(category) || "REVIEW".equals(category)) {
                ps.setString(idx++, category);
            }
            ps.setInt(idx, Math.max(1, limit));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String title = safe(rs.getString("wechat_title"));
                if (empty(title)) title = safe(rs.getString("title"));
                String summary = safe(rs.getString("wechat_summary"));
                String url = safe(rs.getString("article_url"));

                if (empty(url) || url.contains("test_dinghong")) continue;

                java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
                item.put("title", articleCardTitle(category, title));
                item.put("description", empty(summary) ? articleCardDesc(category) : summary);
                item.put("picUrl", "");
                item.put("url", url);
                items.add(item);
            }
        } catch (Exception e) {
            System.out.println("[CONTENT_MENU_DB_ERROR] menu=" + menuName + " error=" + e.getMessage());
        }

        return items;
    }

    private String articleMenuCategory(String menuName) {
        if ("今日推荐".equals(menuName)) return "PREVIEW";
        if ("昨日复盘".equals(menuName)) return "REVIEW";
        return null;
    }

    private boolean articleMenuRequiresToday(String menuName) {
        return "今日推荐".equals(menuName) || "昨日复盘".equals(menuName);
    }

    private boolean epochInBeijingDate(long epochSeconds, java.time.LocalDate date) {
        if (epochSeconds <= 0 || date == null) return false;
        java.time.LocalDate d = java.time.Instant.ofEpochSecond(epochSeconds)
                .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                .toLocalDate();
        return date.equals(d);
    }

    private long firstLong(com.fasterxml.jackson.databind.JsonNode node, String... names) {
        if (node == null || names == null) return 0L;
        for (String name : names) {
            com.fasterxml.jackson.databind.JsonNode v = node.get(name);
            if (v == null || v.isNull()) continue;
            if (v.isNumber()) return v.asLong();
            try {
                String s = v.asText("").trim();
                if (!s.isEmpty()) return Long.parseLong(s);
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    private String nodeText(com.fasterxml.jackson.databind.JsonNode node, String name) {
        if (node == null || name == null) return "";
        com.fasterxml.jackson.databind.JsonNode v = node.get(name);
        return v == null || v.isNull() ? "" : cleanJsonText(v.asText(""));
    }

    public java.util.List<java.util.Map<String, String>> articleNewsItems(String category, int limit) {
        java.util.List<java.util.Map<String, String>> items = articleNewsItemsFromWechat(category, limit);
        if (items != null && !items.isEmpty()) return items;
        return articleNewsItemsFromDb(category, limit);
    }

    private java.util.List<java.util.Map<String, String>> articleNewsItemsFromWechat(String category, int limit) {
        java.util.List<java.util.Map<String, String>> items = new java.util.ArrayList<>();

        try {
            String appId = System.getenv("WECHAT_APPID");
            String secret = System.getenv("WECHAT_SECRET");
            if (empty(appId) || empty(secret)) return items;

            String tokenUrl =
                    "https://api.weixin.qq.com/cgi-bin/token"
                    + "?grant_type=client_credential"
                    + "&appid=" + appId
                    + "&secret=" + secret;

            String tokenJson = readUrl(tokenUrl);
            String token = extractJson(tokenJson, "access_token");
            if (empty(token)) return items;

            String api =
                    "https://api.weixin.qq.com/cgi-bin/freepublish/batchget?access_token="
                    + token;

            String body = "{\"offset\":0,\"count\":20,\"no_content\":1}";
            String json = postJson(api, body);

            String[] parts = json.split("\\\"title\\\":");
            for (int i = 1; i < parts.length; i++) {
                String block = parts[i];
                String title = firstJsonString(block);
                String digest = jsonField(block, "digest");
                String url = jsonField(block, "url").replace("\\/", "/");
                String thumbUrl = jsonField(block, "thumb_url").replace("\\/", "/");

                if (empty(title) || empty(url)) continue;
                if (!articleCategoryMatch(category, title, digest)) continue;

                java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
                item.put("title", articleCardTitle(category, title));
                item.put("description", empty(digest) ? articleCardDesc(category) : digest);
                item.put("picUrl", thumbUrl);
                item.put("url", url);
                items.add(item);

                if (items.size() >= Math.max(1, limit)) break;
            }

        } catch (Exception e) {
            System.out.println("[WECHAT_ARTICLE_NEWS_ERROR] " + e.getMessage());
        }

        return items;
    }

    private java.util.List<java.util.Map<String, String>> articleNewsItemsFromDb(String category, int limit) {
        java.util.List<java.util.Map<String, String>> items = new java.util.ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            String sql =
                    "SELECT id,title,wechat_title,wechat_summary,article_category,article_url,created_at " +
                    "FROM article_task " +
                    "WHERE status <> 'ARCHIVED' ";

            if ("PREVIEW".equals(category) || "REVIEW".equals(category)) {
                sql += "AND article_category=? ";
            }

            sql += "ORDER BY id DESC LIMIT ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            int idx = 1;
            if ("PREVIEW".equals(category) || "REVIEW".equals(category)) {
                ps.setString(idx++, category);
            }
            ps.setInt(idx, Math.max(1, limit));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String title = safe(rs.getString("wechat_title"));
                if (empty(title)) title = safe(rs.getString("title"));
                String summary = safe(rs.getString("wechat_summary"));
                String url = safe(rs.getString("article_url"));

                // 没有正式文章链接时，不做图文卡片，避免用户点进空链接。
                if (empty(url) || url.contains("test_dinghong")) continue;

                java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
                item.put("title", articleCardTitle(category, title));
                item.put("description", empty(summary) ? articleCardDesc(category) : summary);
                item.put("picUrl", "");
                item.put("url", url);
                items.add(item);
            }
        } catch (Exception e) {
            System.out.println("[ARTICLE_DB_NEWS_ERROR] " + e.getMessage());
        }

        return items;
    }

    private boolean articleCategoryMatch(String category, String title, String digest) {
        if (empty(category)) return true;
        boolean review = isReviewArticle(title, digest);
        if ("REVIEW".equals(category)) return review;
        if ("PREVIEW".equals(category)) return !review;
        return true;
    }

    private boolean isReviewArticle(String title, String digest) {
        String s = safe(title) + " " + safe(digest);
        return s.contains("复盘")
                || s.contains("回顾")
                || s.contains("赛后")
                || s.contains("赛果")
                || s.contains("命中")
                || s.contains("未命中")
                || s.contains("验证")
                || s.contains("偏差")
                || s.contains("失误")
                || s.contains("打出");
    }

    private String articleCardTitle(String category, String title) {
        String t = safe(title);
        if (empty(t)) return articleCardDesc(category);
        if ("PREVIEW".equals(category) && !t.contains("推荐") && !t.contains("前瞻") && !t.contains("赛前")) {
            return "今日推荐｜" + t;
        }
        if ("REVIEW".equals(category) && !t.contains("复盘") && !t.contains("回顾") && !t.contains("赛后")) {
            return "昨日复盘｜" + t;
        }
        return t;
    }

    private String articleCardDesc(String category) {
        if ("PREVIEW".equals(category)) return "点击查看顶红体育今日赛事观点";
        if ("REVIEW".equals(category)) return "点击查看顶红体育赛后验证与复盘";
        return "点击查看顶红体育最新文章";
    }

    private String firstJsonString(String block) {
        if (block == null) return "";
        int s = block.indexOf('"');
        if (s < 0) return "";
        StringBuilder out = new StringBuilder();
        boolean esc = false;
        for (int i = s + 1; i < block.length(); i++) {
            char c = block.charAt(i);
            if (esc) {
                out.append(c);
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
            }
        }
        return cleanJsonText(out.toString());
    }

    private String jsonField(String block, String key) {
        if (block == null || key == null) return "";
        String tag = "\\\"" + key + "\\\":\\\"";
        int p = block.indexOf(tag);
        if (p < 0) {
            tag = "\"" + key + "\":\"";
            p = block.indexOf(tag);
        }
        if (p < 0) return "";
        int start = p + tag.length();
        StringBuilder out = new StringBuilder();
        boolean esc = false;
        for (int i = start; i < block.length(); i++) {
            char c = block.charAt(i);
            if (esc) {
                out.append(c);
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
            }
        }
        return cleanJsonText(out.toString());
    }

    private String cleanJsonText(String s) {
        if (s == null) return "";
        return s.replace("\\/", "/")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("&amp;", "&")
                .trim();
    }


    public java.util.List<java.util.Map<String, String>> searchMatchNewsItems(String keyword) {
        java.util.List<java.util.Map<String, String>> items = new java.util.ArrayList<>();
        if (empty(keyword) || isMenuCommand(keyword) || isLiveIndexCommand(keyword)) return items;

        String q = keyword.trim();
        if (q.length() < 2) return items;

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM match_live " +
                    "WHERE show_in_wechat=1 " +
                    "AND (league_name LIKE ? OR home_team LIKE ? OR away_team LIKE ? OR keywords LIKE ?) " +
                    "ORDER BY CASE WHEN live_status='AVAILABLE' THEN 0 WHEN live_status='WAITING' THEN 1 ELSE 2 END, id DESC LIMIT 5"
            );

            String kw = "%" + q + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ps.setString(4, kw);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(matchNewsItem(rs, "查找比赛"));
            }
        } catch (Exception e) {
            System.out.println("[MATCH_SEARCH_NEWS_ERROR] " + e.getMessage());
        }

        return items;
    }

    public java.util.List<java.util.Map<String, String>> todaySportNewsItems(String sport) {
        java.util.List<java.util.Map<String, String>> items = new java.util.ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM match_live " +
                    "WHERE show_in_wechat=1 " +
                    "AND live_status IN ('AVAILABLE','WAITING') " +
                    "ORDER BY CASE WHEN live_status='AVAILABLE' THEN 0 WHEN live_status='WAITING' THEN 1 ELSE 2 END, id DESC LIMIT 30"
            );

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String sportType = safe(rs.getString("sport_type"));
                String league = safe(rs.getString("league_name"));
                String home = safe(rs.getString("home_team"));
                String away = safe(rs.getString("away_team"));

                boolean basketball = isBasketball(sportType, league, home, away);
                boolean football = isFootball(sportType, league, home, away);

                if ("basketball".equals(sport) && !basketball) continue;
                if ("football".equals(sport) && basketball) continue;
                if ("football".equals(sport) && !football && !empty(sportType)) continue;

                items.add(matchNewsItem(rs, "basketball".equals(sport) ? "今日篮球" : "今日足球"));
                if (items.size() >= 8) break;
            }
        } catch (Exception e) {
            System.out.println("[TODAY_SPORT_NEWS_ERROR] " + e.getMessage());
        }

        return items;
    }

    private java.util.Map<String, String> matchNewsItem(ResultSet rs, String source) throws SQLException {
        long id = rs.getLong("id");
        String league = safe(rs.getString("league_name"));
        String home = safe(rs.getString("home_team"));
        String away = safe(rs.getString("away_team"));
        String time = safe(rs.getString("match_time"));
        String status = safe(rs.getString("live_status"));
        String streamKey = safe(rs.getString("stream_key"));
        String streamUrl = safe(rs.getString("stream_url"));
        String picUrl = safe(rs.getString("qrcode_url"));

        if (empty(streamKey)) streamKey = "live_" + id;

        String title = (empty(league) ? "赛事" : league) + "｜" + home + " VS " + away;

        StringBuilder desc = new StringBuilder();
        if (!empty(time)) desc.append("北京时间 ").append(time).append("｜");
        desc.append(liveStatusText(status, streamUrl));
        desc.append("｜点击卡片进入直播页");

        java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
        item.put("title", title);
        item.put("description", desc.toString());
        item.put("picUrl", picUrl);
        item.put("url", "https://live.5q.lol/play.html?key=" + streamKey);
        return item;
    }

    private String extractBlockJsonField(String block, String key) {
        if (block == null || key == null) return "";
        String tag = "\"" + key + "\":\"";
        int p = block.indexOf(tag);
        if (p < 0) return "";
        int s = p + tag.length();
        StringBuilder out = new StringBuilder();
        boolean esc = false;
        for (int i = s; i < block.length(); i++) {
            char c = block.charAt(i);
            if (esc) {
                out.append(c);
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
            }
        }
        return out.toString().replace("\\n", " ").trim();
    }
    private boolean isLiveIndexCommand(String content) {
        return parseLiveIndex(content) > 0;
    }

    private int parseLiveIndex(String content) {
        if (content == null) return -1;

        String s = content.trim();
        if (s.isEmpty()) return -1;

        s = s.replace("１", "1")
                .replace("２", "2")
                .replace("３", "3")
                .replace("４", "4")
                .replace("５", "5")
                .replace("６", "6")
                .replace("７", "7")
                .replace("８", "8")
                .replace("９", "9")
                .replace("０", "0")
                .replaceAll("\\s+", "");

        if (s.startsWith("直播")) {
            s = s.substring(2);
        }

        if (s.startsWith("第")) {
            s = s.substring(1);
        }

        if (s.endsWith("场")) {
            s = s.substring(0, s.length() - 1);
        }

        if (s.matches("^\\d{1,2}$")) {
            try {
                int n = Integer.parseInt(s);
                return n > 0 ? n : -1;
            } catch (Exception ignored) {}
        }

        switch (s) {
            case "一": return 1;
            case "二": return 2;
            case "三": return 3;
            case "四": return 4;
            case "五": return 5;
            case "六": return 6;
            case "七": return 7;
            case "八": return 8;
            case "九": return 9;
            case "十": return 10;
            default: return -1;
        }
    }


    private PreparedStatement recentLiveStatement(Connection conn, int limit) throws SQLException {
        String sql =
                "SELECT * FROM match_live " +
                "WHERE show_in_wechat=1 " +
                "AND live_status IN ('AVAILABLE','WAITING') " +
                "ORDER BY " +
                "CASE " +
                "WHEN live_status='AVAILABLE' AND stream_url IS NOT NULL AND TRIM(stream_url)<>'' THEN 0 " +
                "WHEN live_status='WAITING' THEN 1 " +
                "ELSE 2 END, " +
                "CASE WHEN match_time IS NULL OR TRIM(match_time)='' THEN 1 ELSE 0 END, " +
                "match_time ASC, id DESC LIMIT ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, limit);
        return ps;
    }
    private String recentLiveList() {
        StringBuilder sb = new StringBuilder();
        sb.append("最近直播\n\n");

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = recentLiveStatement(conn, 6);
            ResultSet rs = ps.executeQuery();

            int count = 0;
            while (rs.next()) {
                count++;
                String league = safe(rs.getString("league_name"));
                String home = safe(rs.getString("home_team"));
                String away = safe(rs.getString("away_team"));
                String time = safe(rs.getString("match_time"));
                String status = safe(rs.getString("live_status"));
                String streamUrl = safe(rs.getString("stream_url"));

                sb.append(count).append(". ").append(newsTitle(league, home, away)).append("\n");
                if (!empty(time)) sb.append("时间：").append(time).append("\n");
                sb.append("状态：").append(liveStatusText(status, streamUrl)).append("\n\n");
            }

            if (count == 0) {
                return "当前暂无可观看直播。\n\n你可以稍后再来，或回复“今日足球 / 今日篮球”查看近期赛事。";
            }

            sb.append("回复数字即可获取二维码：");
            for (int i = 1; i <= count; i++) {
                if (i > 1) sb.append(" / ");
                sb.append(i);
            }
            sb.append("\n");
            sb.append("也可以直接回复球队名。");

            return sb.toString().trim();

        } catch (Exception e) {
            System.out.println("[RECENT_LIVE_ERROR] " + e.getMessage());
            return "最近直播暂时无法获取，请稍后再试。";
        }
    }



    private String recentLiveByIndex(String content) {
        int target = parseLiveIndex(content);
        if (target <= 0) return "请输入正确的直播编号，例如：直播1。";

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = recentLiveStatement(conn, 10);
            ResultSet rs = ps.executeQuery();

            int count = 0;
            while (rs.next()) {
                count++;
                if (count == target) {
                    return buildLiveEntryText(rs, count);
                }
            }

            return "没有找到对应直播。\n\n请发送“最近直播”查看当前可选赛事。";

        } catch (Exception e) {
            System.out.println("[RECENT_LIVE_INDEX_ERROR] " + e.getMessage());
            return "直播入口暂时无法获取，请稍后再试。";
        }
    }


    private String getOnlyRecentLiveMediaId() {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = recentLiveStatement(conn, 2);
            ResultSet rs = ps.executeQuery();

            int count = 0;
            String mediaId = "";
            String status = "";
            String streamUrl = "";

            while (rs.next()) {
                count++;
                if (count == 1) {
                    mediaId = safe(rs.getString("wechat_media_id"));
                    status = safe(rs.getString("live_status"));
                    streamUrl = safe(rs.getString("stream_url"));
                }

                if (count > 1) {
                    return "";
                }
            }

            if (count == 1 && "AVAILABLE".equals(status) && !empty(streamUrl) && !empty(mediaId)) {
                return mediaId;
            }

        } catch (Exception e) {
            System.out.println("[ONLY_RECENT_LIVE_MEDIA_ERROR] " + e.getMessage());
        }

        return "";
    }

    private String getRecentLiveMediaIdByIndex(String content) {
        int target = parseLiveIndex(content);
        if (target <= 0) return "";

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = recentLiveStatement(conn, 10);
            ResultSet rs = ps.executeQuery();

            int count = 0;
            while (rs.next()) {
                count++;
                if (count == target) {
                    String status = safe(rs.getString("live_status"));
                    String mediaId = safe(rs.getString("wechat_media_id"));
                    String streamUrl = safe(rs.getString("stream_url"));

                    if ("AVAILABLE".equals(status) && !empty(mediaId) && !empty(streamUrl)) {
                        return mediaId;
                    }

                    return "";
                }
            }

        } catch (Exception e) {
            System.out.println("[RECENT_LIVE_MEDIA_ERROR] " + e.getMessage());
        }

        return "";
    }

    private String buildLiveEntryText(ResultSet rs, int index) throws SQLException {
        String league = safe(rs.getString("league_name"));
        String home = safe(rs.getString("home_team"));
        String away = safe(rs.getString("away_team"));
        String time = safe(rs.getString("match_time"));
        String status = safe(rs.getString("live_status"));
        String streamKey = safe(rs.getString("stream_key"));
        String streamUrl = safe(rs.getString("stream_url"));
        String mediaId = safe(rs.getString("wechat_media_id"));

        if (empty(streamKey)) {
            streamKey = "live_" + rs.getLong("id");
        }

        String playUrl = "https://live.5q.lol/play.html?key=" + streamKey;

        StringBuilder sb = new StringBuilder();
        sb.append(newsTitle(league, home, away)).append("\n");
        if (!empty(time)) sb.append("时间：").append(time).append("\n");
        sb.append("状态：").append(liveStatusText(status, streamUrl)).append("\n\n");

        if ("AVAILABLE".equals(status) && !empty(streamUrl)) {
            if (!empty(mediaId)) {
                sb.append("二维码已发送，请扫码进入直播页。\n");
                sb.append("如页面加载较慢，请点击“刷新直播”。");
            } else {
                sb.append("直播入口：\n").append(playUrl).append("\n\n");
                sb.append("如页面加载较慢，请点击“刷新直播”。");
            }
        } else if ("AVAILABLE".equals(status)) {
            sb.append("直播信号正在接入，请稍后刷新直播页。\n");
            sb.append("入口：\n").append(playUrl);
        } else if ("WAITING".equals(status)) {
            sb.append("比赛尚未开始，开赛前请回来刷新。\n");
            sb.append("入口：\n").append(playUrl);
        } else {
            sb.append("当前暂不可观看，请稍后从“最近直播”重新进入。");
        }

        return sb.toString().trim();
    }



    private String searchMatch(String keyword) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM match_live " +
                    "WHERE league_name LIKE ? OR home_team LIKE ? OR away_team LIKE ? OR keywords LIKE ? " +
                    "ORDER BY id DESC LIMIT 3"
            );

            String kw = "%" + keyword.trim() + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ps.setString(4, kw);

            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder();
            int count = 0;

            while (rs.next()) {
                count++;
                appendMatchLine(sb, rs, count);
            }

            if (count == 0) return "";

            sb.append("如需进入直播页，请发送“最近直播”，再按编号回复，例如：直播1。");
            return sb.toString().trim();

        } catch (Exception e) {
            System.out.println("[MATCH_SEARCH_ERROR] " + e.getMessage());
            return "比赛查询暂时异常，请稍后再试。";
        }
    }

    private String todayAll() {
        return "今日赛事\n\n"
                + removeTitle(todayBySport("football")) + "\n\n"
                + removeTitle(todayBySport("basketball"));
    }

    private String todayBySport(String sport) {
        StringBuilder sb = new StringBuilder();

        if ("basketball".equals(sport)) {
            sb.append("🏀 今日篮球\n\n");
        } else {
            sb.append("⚽ 今日足球\n\n");
        }

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM match_live " +
                    "WHERE show_in_wechat=1 AND live_status IN ('AVAILABLE','WAITING') " +
                    "ORDER BY " +
                    "CASE " +
                    "WHEN live_status='AVAILABLE' AND stream_url IS NOT NULL AND TRIM(stream_url)<>'' THEN 0 " +
                    "WHEN live_status='WAITING' THEN 1 " +
                    "ELSE 2 END, " +
                    "CASE WHEN match_time IS NULL OR TRIM(match_time)='' THEN 1 ELSE 0 END, " +
                    "match_time ASC, id DESC LIMIT 30"
            );

            ResultSet rs = ps.executeQuery();
            int count = 0;

            while (rs.next()) {
                String sportType = safe(rs.getString("sport_type"));
                String league = safe(rs.getString("league_name"));
                String home = safe(rs.getString("home_team"));
                String away = safe(rs.getString("away_team"));

                boolean isBasketball = isBasketball(sportType, league, home, away);

                if ("basketball".equals(sport) && !isBasketball) continue;
                if ("football".equals(sport) && isBasketball) continue;

                count++;
                appendMatchLine(sb, rs, count);
            }

            if (count == 0) {
                sb.append("暂无相关赛事更新。\n\n");
                sb.append("你可以稍后再来，或发送球队名 / 联赛名查询。需要看直播时，请优先发送“最近直播”。");
            } else {
                sb.append("提示：本页是赛事列表；如需进入直播页，请发送“最近直播”并按编号回复。");
            }

        } catch (Exception e) {
            System.out.println("[TODAY_MATCH_ERROR] " + e.getMessage());
            return "今日赛事暂时无法获取，请稍后再试。";
        }

        return sb.toString().trim();
    }

    private void appendMatchLine(StringBuilder sb, ResultSet rs, int count) throws SQLException {
        String league = safe(rs.getString("league_name"));
        String home = safe(rs.getString("home_team"));
        String away = safe(rs.getString("away_team"));
        String time = safe(rs.getString("match_time"));
        String status = safe(rs.getString("live_status"));
        String streamUrl = safe(rs.getString("stream_url"));

        sb.append(count).append(". ").append(newsTitle(league, home, away)).append("\n");

        if (!empty(time)) sb.append("时间：").append(time).append("\n");
        sb.append("状态：").append(liveStatusText(status, streamUrl)).append("\n");

        if ("AVAILABLE".equals(status) || "WAITING".equals(status)) {
            sb.append("入口：发送“最近直播”后按编号进入。\n");
        }

        sb.append("\n");
    }



    private String latestArticles(String category, int limit) {
        StringBuilder sb = new StringBuilder();

        if ("PREVIEW".equals(category)) {
            sb.append("今日推荐\n\n");
        } else if ("REVIEW".equals(category)) {
            sb.append("昨日复盘\n\n");
        } else {
            sb.append("最新文章\n\n");
        }

        try {
            String appId = System.getenv("WECHAT_APPID");
            String secret = System.getenv("WECHAT_SECRET");

            String tokenUrl =
                    "https://api.weixin.qq.com/cgi-bin/token"
                    + "?grant_type=client_credential"
                    + "&appid=" + appId
                    + "&secret=" + secret;

            String tokenJson = readUrl(tokenUrl);

            String token = extractJson(tokenJson, "access_token");

            if (empty(token)) {
                return "公众号文章暂时无法获取，请稍后再试。";
            }

            String api =
                    "https://api.weixin.qq.com/cgi-bin/freepublish/batchget?access_token="
                    + token;

            String body =
                    "{\"offset\":0,\"count\":10,\"no_content\":1}";

            String json = postJson(api, body);

            String[] parts = json.split("\"title\":");

            int count = 0;

            for (int i = 1; i < parts.length; i++) {

                String block = parts[i];

                String title = block.split("\"")[1];

                String digest = "";

                if (block.contains("\"digest\":\"")) {
                    digest = block.split("\"digest\":\"")[1]
                            .split("\"")[0]
                            .replace("\\\n", " ");
                }

                String url = "";

                if (block.contains("\"url\":\"")) {
                    url = block.split("\"url\":\"")[1]
                            .split("\"")[0]
                            .replace("\\/", "/");
                }

                if ("PREVIEW".equals(category)) {
                    if (isReviewArticle(title, digest)) {
                        continue;
                    }
                }

                if ("REVIEW".equals(category)) {
                    if (!isReviewArticle(title, digest)) {
                        continue;
                    }
                }

                count++;

                sb.append(count)
                        .append(". ")
                        .append(title)
                        .append("\n\n");

                if (!empty(digest)) {
                    sb.append(digest)
                            .append("\n\n");
                }

                if (!empty(url)) {
                    sb.append("阅读全文：\n")
                            .append(url)
                            .append("\n\n");
                }

                if (count >= limit) {
                    break;
                }
            }

            if (count == 0) {
                return "暂未找到相关文章，请查看公众号最新推送。";
            }

            return sb.toString().trim();

        } catch (Exception e) {
            System.out.println("[WECHAT_ARTICLE_ERROR] " + e.getMessage());
            return "公众号文章暂时无法获取，请稍后再试。";
        }
    }

    private String readUrl(String urlStr) throws Exception {
        HttpURLConnection conn =
                (HttpURLConnection) new URL(urlStr).openConnection();

        BufferedReader br =
                new BufferedReader(
                        new InputStreamReader(
                                conn.getInputStream(),
                                StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();

        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        br.close();

        return sb.toString();
    }

    private String postJson(String urlStr, String body) throws Exception {

        HttpURLConnection conn =
                (HttpURLConnection) new URL(urlStr).openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

        conn.getOutputStream().write(
                body.getBytes(StandardCharsets.UTF_8));

        BufferedReader br =
                new BufferedReader(
                        new InputStreamReader(
                                conn.getInputStream(),
                                StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();

        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        br.close();

        return sb.toString();
    }

    private String extractJson(String json, String key) {

        String tag = "\"" + key + "\":\"";

        int p = json.indexOf(tag);

        if (p < 0) return "";

        int s = p + tag.length();

        int e = json.indexOf("\"", s);

        if (e < 0) return "";

        return json.substring(s, e);
    }
    private String aiCustomer(String question) {
        try {
            String system =
                    "你是顶红体育公众号AI客服。"
                    + "只回答赛事资讯、直播指引、平台说明、文章查看和使用帮助。"
                    + "禁止荐彩、喊单、承诺赛果、诱导投注。"
                    + "禁止使用稳赢、稳胆、必中、包红、红单、收米、梭哈、重注、上车、跟单等词。"
                    + "回答要简短，像公众号客服。";

            String prompt =
                    "用户问题：" + question + "\n\n"
                    + "请用中文简短回复。"
                    + "如果用户询问具体直播，提醒他发送球队名或点击【看直播】菜单。"
                    + "如果用户询问预测，只能说可以查看【看内容】里的今日推荐和昨日复盘。";

            String result = deepSeekService.chat(system, prompt);

            if (empty(result)) return defaultNotFound();

            return cleanAi(result);

        } catch (Exception e) {
            System.out.println("[AI_CUSTOMER_ERROR] " + e.getMessage());
            return defaultNotFound();
        }
    }



    private String defaultNotFound() {
        return "暂未找到相关内容。\n\n你可以发送“最近直播”查看直播入口，或发送球队名 / 联赛名查询赛事。";
    }

private String welcome() {
        return "欢迎来到顶红体育。\n\n"
                + "看直播：发送“最近直播”，查看当前可观看赛事。\n"
                + "查比赛：发送球队名、联赛名，查询相关赛事。\n"
                + "看内容：点击“今日推荐 / 昨日复盘”，查看最新观点与赛后复盘。\n\n"
                + "常用指令：最近直播、今日足球、今日篮球、查找比赛、直播说明。";
    }

    private String aiWelcome() {
        return "智能客服已接入。\n\n"
                + "你可以直接发送问题，例如：\n"
                + "1. 最近直播怎么进入？\n"
                + "2. 想查某支球队的比赛。\n"
                + "3. 今日推荐在哪里看？\n\n"
                + "如果要看直播，请优先发送“最近直播”。";
    }

    private String searchHelp() {
        return "查找比赛\n\n"
                + "你可以直接发送球队名、联赛名或关键词。\n"
                + "例如：德国、芬兰、巴西、巴拿马、欧冠。\n\n"
                + "如果要进入直播页，请发送“最近直播”，再回复对应编号，例如：直播1。";
    }

    private String liveHelp() {
        return "直播说明\n\n"
                + "1. 发送“最近直播”查看当前可观看赛事。\n"
                + "2. 点击图文卡片可直接进入直播页。\n"
                + "3. 如需二维码，可按提示回复“直播1 / 直播2”。\n"
                + "4. 直播链接固定，线路临时更新时无需更换二维码。\n"
                + "5. 如页面加载较慢，请先点击“刷新直播”。\n\n"
                + "如果没有看到想看的比赛，可以发送球队名或联赛名查询。";
    }

    private String help() {
        return "使用说明\n\n"
                + "看直播：发送“最近直播”，点击图文卡片进入直播页。\n"
                + "查赛事：发送“今日足球 / 今日篮球”，查看赛事列表。\n"
                + "找比赛：发送球队名、联赛名或关键词。\n"
                + "看文章：点击“今日推荐 / 昨日复盘 / 最新文章”。\n"
                + "联系客服：点击“联系人工”。";
    }

    private String contact() {
        return "联系人工\n\n"
                + "如比赛未展示、页面无法播放或多次刷新仍异常，"
                + "请通过直播页“联系专员”或公众号留言说明比赛名称。";
    }

    private String editorRoom() {
        return "顶红体育编辑部\n\n"
                + "这里用于展示顶红体育的赛前观点、赛后复盘和编辑部内容。\n"
                + "当前公众号只保留最新一期赛前推荐与最新一期赛后复盘。\n\n"
                + "发送“今日推荐”查看最新赛前观点。\n"
                + "发送“昨日复盘”查看最新赛后验证。";
    }


    private boolean isBasketball(String sportType, String league, String home, String away) {
        String t = (safe(sportType) + " " + safe(league) + " " + safe(home) + " " + safe(away)).toLowerCase();

        return t.contains("basketball")
                || t.contains("nba")
                || t.contains("cba")
                || t.contains("wnba")
                || t.contains("euroleague")
                || t.contains("eurocup")
                || t.contains("bsl")
                || t.contains("tbf")
                || t.contains("basketbol")
                || t.contains("篮球")
                || t.contains("男篮")
                || t.contains("女篮")
                || t.contains("篮联")
                || t.contains("篮超")
                || t.contains("欧篮")
                || t.contains("土篮")
                || t.contains("土耳其篮")
                || t.contains("费内巴切")
                || t.contains("阿纳多卢")
                || t.contains("艾菲斯")
                || t.contains("湖人")
                || t.contains("勇士")
                || t.contains("凯尔特人")
                || t.contains("独行侠")
                || t.contains("掘金")
                || t.contains("快船")
                || t.contains("太阳")
                || t.contains("雄鹿")
                || t.contains("热火")
                || t.contains("尼克斯")
                || t.contains("森林狼")
                || t.contains("雷霆")
                || t.contains("火箭")
                || t.contains("公牛")
                || t.contains("骑士")
                || t.contains("猛龙")
                || t.contains("辽宁")
                || t.contains("广东")
                || t.contains("新疆")
                || t.contains("浙江");
    }

    private boolean isFootball(String sportType, String league, String home, String away) {
        String t = (safe(sportType) + " " + safe(league) + " " + safe(home) + " " + safe(away)).toLowerCase();

        if (isBasketball(sportType, league, home, away)) return false;

        return t.contains("football")
                || t.contains("soccer")
                || t.contains("足球")
                || t.contains("英超")
                || t.contains("西甲")
                || t.contains("德甲")
                || t.contains("意甲")
                || t.contains("法甲")
                || t.contains("欧冠")
                || t.contains("欧联")
                || t.contains("欧协")
                || t.contains("中超")
                || t.contains("日职")
                || t.contains("韩职")
                || t.contains("巴甲")
                || t.contains("美职")
                || t.contains("杯")
                || t.contains("u19")
                || t.contains("u20")
                || t.contains("u21")
                || t.contains("u23")
                || t.contains("曼联")
                || t.contains("曼城")
                || t.contains("阿森纳")
                || t.contains("切尔西")
                || t.contains("利物浦")
                || t.contains("皇马")
                || t.contains("巴萨")
                || t.contains("巴黎")
                || t.contains("拜仁")
                || t.contains("米兰")
                || t.contains("尤文");
    }

    private boolean isMenuCommand(String content) {
        return "最近直播".equals(content)
                || "直播列表".equals(content)
                || "直播赛事".equals(content)
                || "今日足球".equals(content)
                || "今日篮球".equals(content)
                || "今日比赛".equals(content)
                || "查找比赛".equals(content)
                || "查直播".equals(content)
                || "直播说明".equals(content)
                || "今日推荐".equals(content)
                || "昨日复盘".equals(content)
                || "最新文章".equals(content)
                || "编辑部".equals(content)
                || "智能客服".equals(content)
                || "使用说明".equals(content)
                || "联系人工".equals(content);
    }

    private String removeTitle(String s) {
        if (s == null) return "";
        return s.replace("⚽ 今日足球", "")
                .replace("🏀 今日篮球", "")
                .trim();
    }

    private String cleanAi(String s) {
        if (s == null) return "";

        return s.replace("#", "")
                .replace("*", "")
                .replace("```", "")
                .replace("稳赢", "更倾向")
                .replace("稳胆", "个人倾向")
                .replace("必中", "值得关注")
                .replace("包红", "值得观察")
                .replace("红单", "")
                .replace("跟单", "参考")
                .replace("上车", "关注")
                .replace("收米", "")
                .replace("梭哈", "")
                .replace("重注", "")
                .trim();
    }

    private boolean empty(String s) {
        return s == null || s.trim().isEmpty();
    }



    private String latestReviewSmart() {
        String published = latestArticles("REVIEW", 1);

        if (!empty(published)
                && !published.contains("暂未找到相关文章")
                && !published.contains("公众号文章暂时无法获取")) {
            published = published.replaceFirst("^最新文章", "昨日复盘");
            published = published.replaceFirst("^昨日复盘\n\n1\\.\\s*", "昨日复盘\n\n");
            return published.trim();
        }

        return latestReviewFromDb();
    }

    private String latestReviewFromDb() {
        StringBuilder sb = new StringBuilder();
        sb.append("昨日复盘\n\n");

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT wechat_title,title,wechat_summary,article_url " +
                    "FROM article_task WHERE article_category='REVIEW' " +
                    "ORDER BY id DESC LIMIT 1"
            );

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return "昨日复盘暂未更新。\n\n编辑部正在整理赛后内容，请稍后查看。";
            }

            String title = safe(rs.getString("wechat_title"));
            if (empty(title)) title = safe(rs.getString("title"));

            String summary = safe(rs.getString("wechat_summary"));
            String url = safe(rs.getString("article_url"));

            sb.append(title).append("\n\n");

            if (!empty(summary)) {
                sb.append(summary).append("\n\n");
            }

            if (!empty(url) && !url.contains("test_dinghong")) {
                sb.append("阅读全文：\n").append(url);
            } else {
                sb.append("完整复盘文章正在发布中，请稍后查看公众号最新推送。");
            }

            return sb.toString().trim();

        } catch (Exception e) {
            System.out.println("[REVIEW_DB_ERROR] " + e.getMessage());
            return "昨日复盘暂时无法获取，请稍后再试。";
        }
    }

    private String latestArticleForPreview() {
        String text = latestArticles("PREVIEW", 1);

        if (empty(text)) {
            return "今日推荐暂未更新，请稍后查看。";
        }

        text = text.replaceFirst("^最新文章", "今日推荐");
        text = text.replaceFirst("\n\n1\\.\\s*", "\n\n");

        return text.trim();
    }


    private String latestArticleForReview() {
        String text = latestArticles("REVIEW", 1);

        if (empty(text)) {
            return "昨日复盘暂未更新，请稍后查看。";
        }

        text = text.replaceFirst("^最新文章", "昨日复盘");
        text = text.replaceFirst("\n\n1\\.\\s*", "\n\n");

        return text.trim();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}