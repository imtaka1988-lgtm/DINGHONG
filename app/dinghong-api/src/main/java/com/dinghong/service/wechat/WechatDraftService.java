package com.dinghong.service.wechat;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WechatDraftService {

    private static final String FONT = "/usr/share/fonts/wqy-microhei/wqy-microhei.ttc";
    private static final String COVER_DIR = "/data/dinghong/cover";
    private static final String ARTICLE_IMAGE_DIR = "/data/dinghong/article_images";

    private final DataSource dataSource;

    public WechatDraftService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String createDraft(Long articleId) {
        try {
            Article article = getArticle(articleId);

            if (article == null) {
                return "文章不存在";
            }

            if (!"APPROVED".equals(article.status) && !"DRAFTED".equals(article.status)) {
                return "请先审核通过，再生成公众号草稿";
            }

            ensureDir(COVER_DIR);
            ensureDir(ARTICLE_IMAGE_DIR);

            String coverPath = COVER_DIR + "/article_" + articleId + ".jpg";

            generateCover(article, coverPath);

            String token = getAccessToken();

            String uploadResult = uploadThumb(token, coverPath);

            String thumbMediaId = extract(uploadResult, "\"media_id\":\"", "\"");

            if (thumbMediaId == null || thumbMediaId.isEmpty()) {
                return "封面上传失败：" + uploadResult;
            }

            List<String> inlineImageUrls = prepareInlineImages(token, article);
            String htmlContent = buildStyledHtml(article, inlineImageUrls);

            String draftResult = addDraft(token, article, thumbMediaId, htmlContent);

            String draftMediaId = extract(draftResult, "\"media_id\":\"", "\"");
            if (draftMediaId != null && !draftMediaId.isEmpty()) {
                updateDraftInfo(articleId, "DRAFTED", draftMediaId);
            }

            return draftResult;

        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    private Article getArticle(Long id) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT a.id,a.title,a.match_id,a.author_editor,a.article_category,a.wechat_title,a.wechat_summary," +
                            "a.cover_text,a.cover_headline,a.final_content,a.status,ml.match_time AS match_time," +
                            "COALESCE(NULLIF(a.sport_type,\'\'), ml.sport_type) AS sport_type,ml.league_name AS league_name,ml.home_team AS home_team,ml.away_team AS away_team " +
                            "FROM article_task a LEFT JOIN match_live ml ON a.match_id=ml.id WHERE a.id=?"
            );

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            Article a = new Article();
            a.id = rs.getLong("id");
            a.title = rs.getString("title");
            a.matchId = rs.getLong("match_id");
            if (rs.wasNull()) {
                a.matchId = null;
            }
            a.author = rs.getString("author_editor");
            a.category = rs.getString("article_category");
            a.wechatTitle = rs.getString("wechat_title");
            a.wechatSummary = rs.getString("wechat_summary");
            a.coverText = rs.getString("cover_text");
            a.coverHeadline = rs.getString("cover_headline");
            a.matchTime = rs.getString("match_time");
            a.sportType = rs.getString("sport_type");
            a.leagueName = rs.getString("league_name");
            a.homeTeam = rs.getString("home_team");
            a.awayTeam = rs.getString("away_team");
            a.content = rs.getString("final_content");
            a.status = rs.getString("status");

            if (safe(a.matchTime, "").isEmpty()) {
                a.matchTime = findMatchTimeFromLive(conn, a);
            }

            return a;
        }
    }

    private String findMatchTimeFromLive(Connection conn, Article a) {
        String text = (safe(a.title, "") + " "
                + safe(a.wechatTitle, "") + " "
                + safe(a.wechatSummary, "") + " "
                + safe(a.coverText, "") + " "
                + safe(a.coverHeadline, "") + " "
                + safe(a.content, "")).toLowerCase(Locale.ROOT);

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT league_name,home_team,away_team,match_time FROM match_live " +
                            "WHERE match_time IS NOT NULL AND match_time<>'' " +
                            "ORDER BY id DESC LIMIT 300"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String home = safe(rs.getString("home_team"), "");
                String away = safe(rs.getString("away_team"), "");
                String league = safe(rs.getString("league_name"), "");
                String matchTime = safe(rs.getString("match_time"), "");
                if (home.isEmpty() || away.isEmpty() || matchTime.isEmpty()) {
                    continue;
                }
                String h = home.toLowerCase(Locale.ROOT);
                String aw = away.toLowerCase(Locale.ROOT);
                boolean teamMatched = text.contains(h) && text.contains(aw);
                boolean leagueMatched = !league.isEmpty() && text.contains(league.toLowerCase(Locale.ROOT)) && (text.contains(h) || text.contains(aw));
                if (teamMatched || leagueMatched) {
                    return matchTime;
                }
            }
        } catch (Exception ignored) {
            // 未匹配到直播表时间时，继续从文章标题/正文里提取，不影响草稿生成。
        }
        return "";
    }


    private void generateCover(Article a, String path) throws Exception {
        boolean isReview = isReview(a);
        boolean isBasketball = isBasketball(a);

        String type = isReview ? "赛后复盘" : "赛前观察";
        String sportName = isBasketball ? "篮球" : "足球";
        String sportEn = isBasketball ? "BASKETBALL" : "FOOTBALL";
        String timeText = matchTimeBeijing(a);
        String headline = safe(a.coverHeadline, isReview ? "复盘结论" : "赛前观点");
        String match = safe(a.title, "重点赛事");
        String author = authorName(a.author);

        headline = headline
                .replace("菜鸡互啄", "攻防都不稳")
                .replace("闭眼等平", "谨慎看平")
                .replace("闭眼看", "谨慎看")
                .replace("信息一团乱", "变量较多")
                .replace("主队方向", "主胜")
                .replace("客队方向", "客胜");

        String time = timeText.isEmpty() ? "顶红体育研究室" : timeText.replace("北京时间", "北京时间 ").replace("  ", " ").trim();
        String[] matchLines = splitVisual(match, 15, 2);
        String[] headlineLines = splitVisual(headline, 8, 2);

        String accent = isBasketball ? "#B96A2C" : "#2D7D59";
        String gold = "#B58A3A";
        String dark = "#111827";

        List<String> args = new ArrayList<>();
        Collections.addAll(args,
                "convert",
                "-size", "900x500",
                "gradient:#FFFCF6-#F1E2C6",
                "-font", FONT,

                "-fill", "rgba(255,255,255,0.60)",
                "-draw", "roundrectangle 34,34 866,466 30,30",

                "-stroke", "#D9C08A",
                "-strokewidth", "2",
                "-fill", "none",
                "-draw", "roundrectangle 50,50 850,450 26,26",

                "-stroke", "rgba(181,138,58,0.18)",
                "-strokewidth", "2",
                "-draw", "line 90,366 810,366",
                "-draw", "line 90,402 810,402",
                "-draw", "circle 450,252 450,190",

                "-stroke", accent,
                "-strokewidth", "5",
                "-draw", "line 78,92 78,178",

                "-fill", accent,
                "-stroke", "none",
                "-draw", "roundrectangle 104,76 288,120 18,18",

                "-fill", "#FFFFFF",
                "-gravity", "NorthWest",
                "-pointsize", "25",
                "-annotate", "+126+84", sportName + " · " + type,

                "-fill", "rgba(17,24,39,0.12)",
                "-gravity", "NorthEast",
                "-pointsize", "28",
                "-annotate", "+82+82", sportEn,

                "-fill", gold,
                "-gravity", "NorthWest",
                "-pointsize", "28",
                "-annotate", "+104+142", time,

                "-fill", "#6B5B3E",
                "-gravity", "SouthWest",
                "-pointsize", "22",
                "-annotate", "+104+48", author + " · 顶红体育研究室",

                "-fill", "#7C6F57",
                "-gravity", "SouthEast",
                "-pointsize", "20",
                "-annotate", "+96+48", "专业赛事观察"
        );

        int matchY = matchLines.length == 1 ? 192 : 176;
        for (String line : matchLines) {
            Collections.addAll(args,
                    "-fill", dark,
                    "-gravity", "NorthWest",
                    "-pointsize", "45",
                    "-annotate", "+104+" + matchY, line
            );
            matchY += 56;
        }

        int headlineY = headlineLines.length == 1 ? 320 : 300;
        for (String line : headlineLines) {
            Collections.addAll(args,
                    "-fill", gold,
                    "-gravity", "NorthWest",
                    "-pointsize", "56",
                    "-annotate", "+104+" + headlineY, line
            );
            headlineY += 62;
        }

        Collections.addAll(args,
                "-quality", "88",
                path
        );

        runConvert(args, "封面生成失败");
    }


    private List<String> prepareInlineImages(String token, Article a) {
        List<String> urls = new ArrayList<>();
        int count = inlineImageCount(a);

        for (int i = 1; i <= count; i++) {
            try {
                String imagePath = ARTICLE_IMAGE_DIR + "/article_" + a.id + "_inline_" + i + ".jpg";
                generateInlineImage(a, imagePath, i);
                String result = uploadArticleImage(token, imagePath);
                String url = extract(result, "\"url\":\"", "\"");
                if (url != null && !url.isEmpty()) {
                    urls.add(url.replace("\\/", "/"));
                }
            } catch (Exception ignored) {
                // 插图失败不阻断草稿箱生成。正文模板仍然可用，避免影响发布流程。
            }
        }

        return urls;
    }

    private int inlineImageCount(Article a) {
        return 0;
    }

    private void generateInlineImage(Article a, String path, int index) throws Exception {
        boolean isReview = isReview(a);
        boolean isBasketball = isBasketball(a);
        Random random = new Random((a.id == null ? 0 : a.id) * 31L + index * 997L);

        String[][] palettes = {
                {"#101217", "#B8292F", "#E0A33A"},
                {"#0F1820", "#2D7D59", "#D8B56A"},
                {"#151018", "#7A2E3B", "#C9A45C"},
                {"#111827", "#C46A2D", "#E0A33A"}
        };
        String[] p = palettes[random.nextInt(palettes.length)];
        String bg = p[0];
        String accent = isReview ? p[2] : p[1];
        String accent2 = isBasketball ? "#C46A2D" : "#2D7D59";

        String type = isReview ? "赛后复盘" : "赛前观察";
        String sportName = isBasketball ? "篮球" : "足球";
        String sport = isBasketball ? "BASKETBALL" : "FOOTBALL";
        String sportType = sportName + " · " + type;
        String timeText = matchTimeBeijing(a);
        String theme = index == 1
                ? (isReview ? "关键转折 · 结果回看" : "焦点对决 · 赛前基调")
                : (isReview ? "场面拆解 · 复盘总结" : "关键变量 · 节奏观察");
        String title = safe(a.coverHeadline, safe(a.wechatTitle, safe(a.title, "顶红体育赛事观察")));
        String[] titleLines = splitVisual(title, 12, 2);

        List<String> args = new ArrayList<>();
        Collections.addAll(args,
                "convert",
                "-size", "900x500",
                "xc:" + bg,
                "-font", FONT,
                "-fill", "rgba(255,255,255,0.04)",
                "-draw", "rectangle 46,46 854,454",
                "-stroke", "rgba(255,255,255,0.12)",
                "-strokewidth", "2",
                "-fill", "none",
                "-draw", "roundrectangle 62,62 838,438 22,22",
                "-stroke", accent,
                "-strokewidth", "4",
                "-draw", "line 94,112 806,112",
                "-stroke", "rgba(255,255,255,0.10)",
                "-strokewidth", "2",
                "-draw", "line 94,374 806,374"
        );

        if (isBasketball) {
            Collections.addAll(args,
                    "-stroke", "rgba(255,255,255,0.12)",
                    "-strokewidth", "3",
                    "-draw", "circle 450,250 450,150",
                    "-draw", "arc 220,145 680,525 200,340",
                    "-draw", "arc 220,-25 680,355 20,160",
                    "-stroke", accent2,
                    "-strokewidth", "5",
                    "-draw", "circle 742,250 742,214"
            );
        } else {
            Collections.addAll(args,
                    "-stroke", "rgba(255,255,255,0.12)",
                    "-strokewidth", "3",
                    "-draw", "rectangle 150,150 750,350",
                    "-draw", "line 450,150 450,350",
                    "-draw", "circle 450,250 450,196",
                    "-stroke", accent2,
                    "-strokewidth", "5",
                    "-draw", "circle 740,250 740,220"
            );
        }

        Collections.addAll(args,
                "-fill", accent,
                "-stroke", "none",
                "-gravity", "NorthWest",
                "-pointsize", "27",
                "-annotate", "+92+78", "顶红体育",
                "-fill", "#BFC5CC",
                "-gravity", "NorthEast",
                "-pointsize", "22",
                "-annotate", "+92+82", sport,
                "-fill", accent,
                "-gravity", "Center",
                "-pointsize", "40",
                "-annotate", "+0-118", sportType,
                "-fill", timeText.isEmpty() ? "rgba(255,255,255,0)" : "#D8B56A",
                "-gravity", "Center",
                "-pointsize", "24",
                "-annotate", "+0-76", timeText
        );

        int y = titleLines.length == 1 ? -30 : -54;
        for (String line : titleLines) {
            Collections.addAll(args,
                    "-fill", "#FFFFFF",
                    "-gravity", "Center",
                    "-pointsize", "54",
                    "-annotate", "+0" + signed(y), line
            );
            y += 62;
        }

        Collections.addAll(args,
                "-fill", "#E4E4E4",
                "-gravity", "Center",
                "-pointsize", "28",
                "-annotate", "+0+116", theme,
                "-fill", "#AEB4BC",
                "-gravity", "South",
                "-pointsize", "20",
                "-annotate", "+0+58", (timeText.isEmpty() ? "赛事氛围示意图 · 非新闻版权照片" : timeText + " · 赛事氛围示意图"),
                path
        );

        runConvert(args, "正文插图生成失败");
    }

    private void runConvert(List<String> args, String errorMessage) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);
        Process p = pb.start();
        int code = p.waitFor();

        if (code != 0) {
            String err = readQuietly(p.getErrorStream());
            throw new RuntimeException(errorMessage + (err.isEmpty() ? "" : "：" + err));
        }
    }

    private String getAccessToken() throws Exception {
        String appid = System.getenv("WECHAT_APPID");
        String secret = System.getenv("WECHAT_SECRET");

        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" +
                appid + "&secret=" + secret;

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");

        String json = read(conn.getInputStream());

        return extract(json, "\"access_token\":\"", "\"");
    }

    private String uploadThumb(String token, String filePath) throws Exception {
        return uploadMultipart(
                "https://api.weixin.qq.com/cgi-bin/material/add_material?access_token=" + token + "&type=thumb",
                filePath,
                "image/jpeg"
        );
    }

    private String uploadArticleImage(String token, String filePath) throws Exception {
        return uploadMultipart(
                "https://api.weixin.qq.com/cgi-bin/media/uploadimg?access_token=" + token,
                filePath,
                "image/jpeg"
        );
    }

    private String uploadMultipart(String targetUrl, String filePath, String contentType) throws Exception {
        String boundary = "----DingHongBoundary" + System.currentTimeMillis();

        URL url = new URL(targetUrl);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        File file = new File(filePath);

        try (OutputStream out = conn.getOutputStream();
             FileInputStream in = new FileInputStream(file)) {

            out.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            out.write(("Content-Disposition: form-data; name=\"media\"; filename=\"" + file.getName() + "\"\r\n").getBytes("UTF-8"));
            out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes("UTF-8"));

            byte[] buf = new byte[4096];
            int len;

            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }

            out.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
        }

        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();

        return read(is);
    }

    private String addDraft(String token, Article a, String thumbMediaId, String htmlContent) throws Exception {
        URL url = new URL("https://api.weixin.qq.com/cgi-bin/draft/add?access_token=" + token);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        String body =
                "{ \"articles\": [ {" +
                        "\"title\":\"" + json(buildWechatTitle(a)) + "\"," +
                        "\"author\":\"顶红体育编辑部\"," +
                        "\"digest\":\"" + json(buildDigest(a)) + "\"," +
                        "\"content\":\"" + json(htmlContent) + "\"," +
                        "\"thumb_media_id\":\"" + json(thumbMediaId) + "\"," +
                        "\"need_open_comment\":0," +
                        "\"only_fans_can_comment\":0" +
                        "} ] }";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }

        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();

        return read(is);
    }

    private void updateStatus(Long id, String status) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE article_task SET status=? WHERE id=?"
            );
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private void updateDraftInfo(Long id, String status, String draftMediaId) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE article_task SET status=?, wechat_draft_media_id=? WHERE id=?"
            );
            ps.setString(1, status);
            ps.setString(2, draftMediaId);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public String publishDraft(Long articleId) {
        try {
            PublishState state = getPublishState(articleId);
            if (state == null) {
                return "文章不存在";
            }

            if (!empty(state.articleUrl) && "PUBLISHED".equals(state.status)) {
                return "published:" + state.articleUrl;
            }

            if (empty(state.draftMediaId)) {
                return "未找到微信草稿 media_id。请先生成公众号草稿；如果这是旧草稿，请重新生成公众号草稿后再发布。";
            }

            String token = getAccessToken();
            if (empty(token)) {
                return "获取微信 access_token 失败，请检查公众号配置。";
            }

            String result = submitFreePublish(token, state.draftMediaId);
            int errcode = extractErrCode(result);
            if (errcode != 0) {
                updatePublishFailure(articleId, result);
                return "发布提交失败：" + result;
            }

            String publishId = extract(result, "\"publish_id\":\"", "\"");
            if (empty(publishId)) {
                publishId = extractNumberString(result, "publish_id");
            }

            if (empty(publishId)) {
                updatePublishFailure(articleId, result);
                return "发布提交失败：微信没有返回 publish_id。" + result;
            }

            updatePublishStarted(articleId, publishId);

            String check = queryPublishStatus(articleId);
            if (check != null && check.startsWith("published:")) {
                return check;
            }

            return "发布已提交，publish_id=" + publishId + "。如果暂未返回链接，请稍后点击“查询发布状态”。";

        } catch (Exception e) {
            try {
                updatePublishFailure(articleId, e.getMessage());
            } catch (Exception ignored) {}
            return "发布异常：" + e.getMessage();
        }
    }

    public String queryPublishStatus(Long articleId) {
        try {
            PublishState state = getPublishState(articleId);
            if (state == null) {
                return "文章不存在";
            }

            if (!empty(state.articleUrl) && "PUBLISHED".equals(state.status)) {
                return "published:" + state.articleUrl;
            }

            if (empty(state.publishId)) {
                return "未找到 publish_id。请先点击“发布到公众号”。";
            }

            String token = getAccessToken();
            if (empty(token)) {
                return "获取微信 access_token 失败，请检查公众号配置。";
            }

            String result = getFreePublishStatus(token, state.publishId);
            int errcode = extractErrCode(result);
            if (errcode != 0) {
                updatePublishFailure(articleId, result);
                return "查询发布状态失败：" + result;
            }

            String articleUrl = extract(result, "\"article_url\":\"", "\"");
            if (empty(articleUrl)) {
                articleUrl = extract(result, "\"url\":\"", "\"");
            }
            articleUrl = cleanWechatUrl(articleUrl);

            if (!empty(articleUrl)) {
                updatePublished(articleId, articleUrl);
                return "published:" + articleUrl;
            }

            String publishStatus = extractNumberString(result, "publish_status");
            if (!empty(publishStatus)) {
                return "发布状态：" + publishStatus + "。微信暂未返回文章链接，请稍后再次查询。";
            }

            return "发布处理中。微信暂未返回文章链接，请稍后再次查询。";

        } catch (Exception e) {
            return "查询发布状态异常：" + e.getMessage();
        }
    }

    private String submitFreePublish(String token, String draftMediaId) throws Exception {
        return postJson(
                "https://api.weixin.qq.com/cgi-bin/freepublish/submit?access_token=" + token,
                "{\"media_id\":\"" + json(draftMediaId) + "\"}"
        );
    }

    private String getFreePublishStatus(String token, String publishId) throws Exception {
        return postJson(
                "https://api.weixin.qq.com/cgi-bin/freepublish/get?access_token=" + token,
                "{\"publish_id\":\"" + json(publishId) + "\"}"
        );
    }

    private String postJson(String targetUrl, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }

        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return read(is);
    }

    private PublishState getPublishState(Long articleId) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id,status,wechat_draft_media_id,wechat_publish_id,article_url FROM article_task WHERE id=?"
            );
            ps.setLong(1, articleId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }

            PublishState s = new PublishState();
            s.id = rs.getLong("id");
            s.status = rs.getString("status");
            s.draftMediaId = rs.getString("wechat_draft_media_id");
            s.publishId = rs.getString("wechat_publish_id");
            s.articleUrl = rs.getString("article_url");
            return s;
        }
    }

    private void updatePublishStarted(Long articleId, String publishId) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE article_task SET status='PUBLISHING', wechat_publish_id=? WHERE id=?"
            );
            ps.setString(1, publishId);
            ps.setLong(2, articleId);
            ps.executeUpdate();
        }
    }

    private void updatePublished(Long articleId, String articleUrl) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE article_task SET status='PUBLISHED', article_url=?, publish_time=NOW() WHERE id=?"
            );
            ps.setString(1, articleUrl);
            ps.setLong(2, articleId);
            ps.executeUpdate();
        }
    }

    private void updatePublishFailure(Long articleId, String reason) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE article_task SET status='PUB_FAILED' WHERE id=?"
            );
            ps.setLong(1, articleId);
            ps.executeUpdate();
        }
        System.out.println("[WECHAT_PUBLISH_FAIL] articleId=" + articleId + " reason=" + reason);
    }

    private int extractErrCode(String jsonText) {
        if (jsonText == null) return 0;
        Matcher m = Pattern.compile("\"errcode\"\\s*:\\s*(-?\\d+)").matcher(jsonText);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private String extractNumberString(String jsonText, String key) {
        if (jsonText == null || key == null) return "";
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"?([^\",}]+)\"?").matcher(jsonText);
        if (m.find()) {
            return cleanWechatUrl(m.group(1));
        }
        return "";
    }

    private String cleanWechatUrl(String s) {
        if (s == null) return "";
        return s.replace("\\/", "/")
                .replace("\\u0026", "&")
                .replace("&amp;", "&")
                .trim();
    }

    private boolean empty(String s) {
        return s == null || s.trim().isEmpty();
    }


    private String buildWechatTitle(Article a) {
        String title = safe(a.wechatTitle, a.title);

        if (isReview(a)
                && !title.startsWith("复盘：")
                && !title.startsWith("复盘:")) {
            title = "复盘：" + title;
        }

        title = withSportTitlePrefix(a, title);

        return limit(title, 64);
    }

    private String withSportTitlePrefix(Article a, String title) {
        String t = stripSportPrefix(safe(title, ""));
        String time = matchTimeShort(a);
        String prefix = (isBasketball(a) ? "篮球" : "足球") + "｜";
        if (!time.isEmpty() && !t.contains(time)) {
            prefix += time + "｜";
        }
        return prefix + t;
    }

    private String stripSportPrefix(String title) {
        String t = safe(title, "");
        if (t.startsWith("篮球｜") || t.startsWith("足球｜") || t.startsWith("篮球|") || t.startsWith("足球|")) {
            return t.substring(3).trim();
        }
        if (t.startsWith("【篮球】") || t.startsWith("【足球】")) {
            return t.substring(4).trim();
        }
        return t;
    }

    private String buildDigest(Article a) {
        String digest = safe(a.wechatSummary, "");
        if (digest.isEmpty()) {
            digest = firstMeaningfulLine(a.content);
        }
        String timeText = matchTimeBeijing(a);
        String prefix = (isBasketball(a) ? "篮球" : "足球") + (isReview(a) ? "复盘" : "赛前观察") + "｜";
        if (!timeText.isEmpty()) {
            prefix += timeText + "｜";
        }
        String clean = digest.replace("\n", " ").replace("\r", " ");
        if (!clean.startsWith(prefix)) {
            clean = prefix + clean;
        }
        return limit(clean, 120);
    }

    private String buildStyledHtml(Article a, List<String> imageUrls) {
        String raw = a.content == null ? "" : a.content.replace("\r", "");
        String[] lines = raw.split("\n");
        List<String> contentLines = new ArrayList<>();

        for (String line : lines) {
            String cleaned = cleanMarkdown(line);
            if (!cleaned.trim().isEmpty()) {
                contentLines.add(cleaned.trim());
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("<section style=\"margin:0 auto;padding:0 2px;color:#2b2b2b;font-size:15px;line-height:1.86;\">");
        sb.append(topBrandBlock(a));

        // 观察方向放在最上面，先告诉读者这篇文章看什么
        sb.append(matchCard(a));

        // 今日看法紧跟观察方向，核心观点前置
        String frontTodayView = buildFrontTodayViewCard(a, contentLines);
        if (!frontTodayView.isEmpty() && !isReview(a)) {
            sb.append(frontTodayView);
        }

        // 导语不再放开头，避免首屏废话太多，改到正文后面展示
        String intro = safe(a.wechatSummary, firstMeaningfulLine(a.content));

        if (imageUrls.size() > 0) {
            sb.append(imageBlock(imageUrls.get(0), isReview(a) ? "赛后复盘配图" : "赛前观察配图"));
        }

        int image2After = Math.max(3, contentLines.size() / 2);
        boolean image2Inserted = false;
        int paragraphIndex = 0;

        for (String line : contentLines) {
            if (isFrontTodayViewLine(line) && !isReview(a)) {
                continue;
            }

            if (shouldSkipDuplicateIntro(line, intro)) {
                continue;
            }

            if (isHeading(line)) {
                sb.append(headingBlock(line));
            } else if (isRiskLine(line)) {
                sb.append(infoCard("理性观赛", line, "#FFF9E8", "#D8A832"));
            } else if (isConclusionLine(line)) {
                sb.append(infoCard(isReview(a) ? "复盘结论" : "编辑部结论", line, "#F3F5F7", "#333333"));
            } else if (isKeyLine(line)) {
                sb.append(infoCard("重点观察", line, "#FFF3F3", "#B8292F"));
            } else if (isDataLine(line)) {
                sb.append(infoCard("数据提示", line, "#F6F7F9", "#6B7280"));
            } else {
                sb.append(paragraph(line));
            }

            paragraphIndex++;
            if (!image2Inserted && imageUrls.size() > 1 && paragraphIndex >= image2After) {
                sb.append(imageBlock(imageUrls.get(1), isReview(a) ? "关键场面示意" : "关键变量示意"));
                image2Inserted = true;
            }
        }

        if (!intro.isEmpty()) {
            sb.append(infoCard("导语", intro, "#FFF7F0", "#B8292F"));
        }

        sb.append(footerBlock());
        sb.append("</section>");

        return sb.toString();
    }



    private String buildFrontTodayViewCard(Article a, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        List<String> items = new ArrayList<>();
        boolean on = false;

        for (String line : lines) {
            if (line == null) {
                continue;
            }

            String t = line.trim();

            if (t.startsWith("今日看法")) {
                on = true;
                continue;
            }

            if (on) {
                if (t.startsWith("PS") || t.startsWith("比赛时间") || t.length() == 0) {
                    break;
                }

                if (isFrontTodayViewLine(t)) {
                    items.add(t);
                    continue;
                }

                if (t.contains("：") || t.contains(":")) {
                    items.add(t);
                    continue;
                }

                break;
            }
        }

        if (items.isEmpty()) {
            return "";
        }

        String accent = isBasketball(a) ? "#B96A2C" : "#2D7D59";
        StringBuilder sb = new StringBuilder();

        sb.append("<section style=\"margin:15px 0 17px;padding:15px 16px;border-radius:15px;background:#FFFCF6;border:1px solid #E5D3AC;box-shadow:0 8px 22px rgba(90,66,28,.07);\">");
        sb.append("<p style=\"margin:0 0 12px;font-size:15px;font-weight:800;color:#8A6A25;letter-spacing:.5px;\">今日看法</p>");

        for (String item : items) {
            String label = "";
            String value = item;

            int idx = item.indexOf("：");
            if (idx < 0) {
                idx = item.indexOf(":");
            }

            if (idx > 0) {
                label = item.substring(0, idx).trim();
                value = item.substring(idx + 1).trim();
            }

            label = normalizeTodayLabel(label);
            value = normalizeTodayValue(value);

            if (label.isEmpty()) {
                sb.append("<p style=\"margin:7px 0;font-size:15px;line-height:1.75;color:#111827;\">")
                        .append(e(value))
                        .append("</p>");
            } else {
                sb.append("<section style=\"margin:8px 0;padding:9px 10px;border-radius:10px;background:#FFFFFF;border:1px solid #EFE2C7;\">")
                        .append("<span style=\"display:inline-block;min-width:78px;font-size:13px;font-weight:800;color:")
                        .append(accent)
                        .append(";\">")
                        .append(e(label))
                        .append("</span>")
                        .append("<span style=\"font-size:15px;font-weight:800;color:#111827;\">")
                        .append(e(value))
                        .append("</span>")
                        .append("</section>");
            }
        }

        sb.append("<p style=\"margin:12px 0 0;font-size:12px;color:#7C6F57;line-height:1.6;\">观点基于赛前资料与盘口信息整理，仅作赛事观察。</p>");
        sb.append("</section>");

        return sb.toString();
    }


    private boolean isFrontTodayViewLine(String line) {
        if (line == null) {
            return false;
        }

        String t = line.trim();

        return t.startsWith("今日看法")
                || t.startsWith("主任方向")
                || t.startsWith("主任看法")
                || t.startsWith("让分看法")
                || t.startsWith("大小球")
                || t.startsWith("大小分")
                || t.startsWith("推荐比分")
                || t.startsWith("分差参考")
                || t.startsWith("方向")
                || t.startsWith("进球数")
                || t.startsWith("总分")
                || t.startsWith("比分参考");
    }

    private String normalizeTodayLabel(String label) {
        if (label == null) {
            return "";
        }

        String t = label.trim();

        if ("方向".equals(t)) return "主任方向";
        if ("进球数/总分".equals(t)) return "大小球";
        if ("总分".equals(t)) return "大小分";
        if ("进球数".equals(t)) return "大小球";
        if ("比分参考".equals(t)) return "推荐比分";

        return t;
    }

    private String normalizeTodayValue(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .replace("主任主任方向", "主任方向")
                .replace("大分方向", "大分")
                .replace("小分方向", "小分")
                .replace("主队方向", "主胜")
                .replace("客队方向", "客胜")
                .replace("双方分差不大", "分差预计在个位数");
    }



    private String topBrandBlock(Article a) {
        String type = isReview(a) ? "赛后复盘" : "赛前观察";
        String sport = isBasketball(a) ? "篮球" : "足球";
        String accent = isBasketball(a) ? "#B96A2C" : "#2D7D59";
        String timeText = matchTimeBeijing(a);
        String timeLine = timeText.isEmpty()
                ? ""
                : "<p style=\"margin:8px 0 0;font-size:14px;font-weight:700;color:#9A7432;\">北京时间 " + e(timeText.replace("北京时间", "").trim()) + "</p>";

        return "<section style=\"margin:0 0 14px;padding:16px 17px;border-radius:15px;background:linear-gradient(135deg,#FFFCF6,#F6EBD8);border:1px solid #E5D3AC;box-shadow:0 8px 22px rgba(90,66,28,.08);\">" +
                "<p style=\"margin:0;display:inline-block;padding:4px 10px;border-radius:999px;background:" + accent + ";font-size:13px;font-weight:800;color:#ffffff;\">" + sport + " · " + type + "</p>" +
                timeLine +
                "<p style=\"margin:12px 0 0;font-size:21px;font-weight:800;line-height:1.42;color:#111827;\">" + e(safe(a.title, "重点赛事观察")) + "</p>" +
                "<p style=\"margin:8px 0 0;font-size:13px;color:#6B5B3E;\">顶红体育研究室｜专业赛事观察</p>" +
                "</section>";
    }


    private String matchCard(Article a) {
        String type = (isBasketball(a) ? "篮球" : "足球") + (isReview(a) ? "复盘方向" : "观察方向");
        String timeText = matchTimeBeijing(a);
        String tip = isReview(a)
                ? "围绕赛果、关键转折和赛前判断回看展开，不硬夸、不回避偏差。"
                : "围绕阵容、节奏、状态和关键变量展开，仅作赛前观察参考。";
        if (!timeText.isEmpty()) {
            tip = "比赛时间：" + timeText + "。" + tip;
        }
        return infoCard(type, tip, "#F8F8F8", isReview(a) ? "#E0A33A" : "#B8292F");
    }

    private String infoCard(String label, String text, String bg, String border) {
        return "<section style=\"margin:18px 0;padding:13px 15px;border-radius:10px;background:" + bg + ";border-left:4px solid " + border + ";\">" +
                "<p style=\"margin:0 0 6px;font-size:13px;font-weight:700;color:" + border + ";\">" + e(label) + "</p>" +
                "<p style=\"margin:0;font-size:15px;line-height:1.85;color:#333333;\">" + e(text) + "</p>" +
                "</section>";
    }

    private String headingBlock(String line) {
        String title = cleanHeading(line);
        return "<section style=\"margin:26px 0 12px;\">" +
                "<p style=\"margin:0;padding:0 0 8px;border-bottom:1px solid #E6E6E6;font-size:18px;font-weight:700;color:#111827;\">" + e(title) + "</p>" +
                "</section>";
    }

    private String paragraph(String line) {
        return "<p style=\"margin:13px 0;font-size:15px;line-height:1.9;color:#333333;text-align:justify;\">" + e(line) + "</p>";
    }

    private String imageBlock(String url, String caption) {
        return "<section style=\"margin:22px 0 20px;\">" +
                "<img src=\"" + e(url) + "\" style=\"display:block;width:100%;border-radius:10px;\"/>" +
                "<p style=\"margin:7px 0 0;text-align:center;font-size:12px;color:#999999;\">图｜" + e(caption) + "</p>" +
                "</section>";
    }

    private String footerBlock() {
        return "<section style=\"margin:26px 0 0;padding:13px 15px;border-radius:10px;background:#FAFAFA;color:#666666;\">" +
                "<p style=\"margin:0;font-size:13px;line-height:1.8;\">本文为顶红体育赛事观察内容，重点用于赛前阅读与赛后复盘参考，不构成任何结果承诺。临场阵容、伤停、节奏和突发事件都可能影响比赛走向。</p>" +
                "</section>";
    }

    private boolean shouldSkipDuplicateIntro(String line, String intro) {
        if (intro == null || intro.trim().isEmpty()) return false;
        String a = plainText(line);
        String b = plainText(intro);
        return a.length() > 12 && b.length() > 12 && (a.contains(limit(b, 18)) || b.contains(limit(a, 18)));
    }

    private boolean isHeading(String line) {
        String s = line.trim();
        return s.matches("^[一二三四五六七八九十]+[、.．].+")
                || s.matches("^[0-9]+[、.．].+")
                || s.startsWith("#")
                || s.startsWith("【") && s.endsWith("】")
                || s.length() <= 18 && (s.contains("赛前") || s.contains("复盘") || s.contains("关键") || s.contains("结论") || s.contains("走势") || s.contains("阵容") || s.contains("数据"));
    }

    private boolean isKeyLine(String line) {
        String s = line.replace(" ", "");
        return s.contains("核心观察") || s.contains("核心观点") || s.contains("重点观察") || s.contains("关键变量") || s.contains("关键点") || s.contains("胜负手") || s.contains("转折点");
    }

    private boolean isDataLine(String line) {
        String s = line.replace(" ", "");
        return s.contains("数据") || s.contains("比分") || s.contains("近") && s.contains("场") || s.contains("控球") || s.contains("射门") || s.contains("篮板") || s.contains("助攻");
    }

    private boolean isConclusionLine(String line) {
        String s = line.replace(" ", "");
        return s.contains("编辑部结论") || s.contains("复盘结论") || s.contains("综合来看") || s.contains("总体来看") || s.contains("最终判断") || s.contains("一句话总结");
    }

    private boolean isRiskLine(String line) {
        String s = line.replace(" ", "");
        return s.contains("风险") || s.contains("理性") || s.contains("仅作") || s.contains("不构成") || s.contains("临场") || s.contains("伤停变化");
    }

    private String cleanMarkdown(String line) {
        if (line == null) return "";
        String s = line.trim();
        s = s.replace("**", "").replace("__", "");
        while (s.startsWith("#")) {
            s = s.substring(1).trim();
        }
        if (s.startsWith("- ") || s.startsWith("* ")) {
            s = s.substring(2).trim();
        }
        return s;
    }

    private String cleanHeading(String line) {
        String s = cleanMarkdown(line);
        if (s.startsWith("【") && s.endsWith("】") && s.length() > 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private String firstMeaningfulLine(String text) {
        if (text == null) return "";
        for (String line : text.replace("\r", "").split("\n")) {
            String s = cleanMarkdown(line);
            if (plainText(s).length() >= 16) {
                return limit(s, 120);
            }
        }
        return "";
    }

    private boolean isReview(Article a) {
        return "REVIEW".equalsIgnoreCase(safe(a.category, ""));
    }


    private boolean isBasketball(Article a) {
        String text = (
                safe(a.title, "") + " " +
                safe(a.wechatTitle, "") + " " +
                safe(a.wechatSummary, "") + " " +
                safe(a.coverText, "") + " " +
                safe(a.coverHeadline, "") + " " +
                safe(a.content, "")
        ).toLowerCase(Locale.ROOT);

        return isBasketballKeywordText(text);
    }

    private boolean isBasketballKeywordText(String text) {
        if (text == null) {
            return false;
        }

        String t = text.toLowerCase(Locale.ROOT);

        return t.contains("篮球")
                || t.contains("男篮")
                || t.contains("女篮")
                || t.contains("nba")
                || t.contains("wnba")
                || t.contains("cba")
                || t.contains("basketball")

                // WNBA
                || t.contains("印第安纳狂热")
                || t.contains("狂热")
                || t.contains("亚特兰大梦想")
                || t.contains("梦想")
                || t.contains("indiana fever")
                || t.contains("fever")
                || t.contains("atlanta dream")
                || t.contains("dream")
                || t.contains("明尼苏达山猫")
                || t.contains("山猫")
                || t.contains("minnesota lynx")
                || t.contains("lynx")
                || t.contains("金州女武神")
                || t.contains("女武神")
                || t.contains("golden state valkyries")
                || t.contains("valkyries")
                || t.contains("芝加哥天空")
                || t.contains("chicago sky")
                || t.contains("康涅狄格太阳")
                || t.contains("connecticut sun")
                || t.contains("洛杉矶火花")
                || t.contains("los angeles sparks")
                || t.contains("达拉斯飞翼")
                || t.contains("dallas wings")
                || t.contains("菲尼克斯水星")
                || t.contains("phoenix mercury")
                || t.contains("波特兰火焰")
                || t.contains("portland fire")

                // NBA 常用队
                || t.contains("湖人")
                || t.contains("勇士")
                || t.contains("凯尔特人")
                || t.contains("独行侠")
                || t.contains("尼克斯")
                || t.contains("马刺")
                || t.contains("掘金")
                || t.contains("快船")
                || t.contains("太阳")
                || t.contains("雄鹿")
                || t.contains("热火")
                || t.contains("森林狼")
                || t.contains("雷霆")
                || t.contains("76人")
                || t.contains("国王")
                || t.contains("灰熊")
                || t.contains("火箭")
                || t.contains("公牛")
                || t.contains("骑士")
                || t.contains("猛龙")
                || t.contains("步行者")
                || t.contains("老鹰")
                || t.contains("黄蜂")
                || t.contains("鹈鹕")
                || t.contains("开拓者")
                || t.contains("爵士")
                || t.contains("魔术")
                || t.contains("奇才")
                || t.contains("活塞")
                || t.contains("篮网")

                // CBA / 国家队
                || t.contains("辽宁")
                || t.contains("广东")
                || t.contains("新疆")
                || t.contains("广厦")
                || t.contains("浙江")
                || t.contains("北京首钢")
                || t.contains("上海男篮")
                || t.contains("深圳男篮")
                || t.contains("中国男篮")
                || t.contains("中国女篮");
    }



    private String matchTimeBeijing(Article a) {
        String shortTime = matchTimeShort(a);
        return shortTime.isEmpty() ? "" : "北京时间 " + shortTime;
    }

    private String matchTimeShort(Article a) {
        String raw = safe(a.matchTime, "");
        if (raw.isEmpty()) {
            raw = extractMatchTimeFromText(a);
        }
        return normalizeMatchTime(raw);
    }

    private String extractMatchTimeFromText(Article a) {
        String text = safe(a.title, "") + " "
                + safe(a.wechatTitle, "") + " "
                + safe(a.wechatSummary, "") + " "
                + safe(a.coverText, "") + " "
                + safe(a.coverHeadline, "") + " "
                + safe(a.content, "");

        String[][] patterns = {
                {"(20[0-9]{2})[-/年.](0?[1-9]|1[0-2])[-/月.](0?[1-9]|[12][0-9]|3[01])日?[ T]*(?:周[一二三四五六日天])?\\s*(20|21|22|23|[01]?[0-9])[:：]([0-5][0-9])", "ymdhm"},
                {"(0?[1-9]|1[0-2])月(0?[1-9]|[12][0-9]|3[01])日?\\s*(?:周[一二三四五六日天])?\\s*(20|21|22|23|[01]?[0-9])[:：]([0-5][0-9])", "mdhm"},
                {"(0?[1-9]|1[0-2])[-/.](0?[1-9]|[12][0-9]|3[01])\\s+(20|21|22|23|[01]?[0-9])[:：]([0-5][0-9])", "mdhm"}
        };

        for (String[] item : patterns) {
            Matcher m = Pattern.compile(item[0]).matcher(text);
            if (m.find()) {
                if ("ymdhm".equals(item[1])) {
                    return m.group(2) + "-" + m.group(3) + " " + m.group(4) + ":" + m.group(5);
                }
                return m.group(1) + "-" + m.group(2) + " " + m.group(3) + ":" + m.group(4);
            }
        }
        return "";
    }

    private String normalizeMatchTime(String raw) {
        String s = safe(raw, "").trim();
        if (s.isEmpty()) return "";
        s = s.replace("T", " ").replace("/", "-").replace(".", "-")
                .replace("年", "-").replace("月", "-").replace("日", " ")
                .replace("：", ":").replaceAll("\\s+", " ").trim();

        Matcher full = Pattern.compile("(?:20[0-9]{2})-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])\\s+(20|21|22|23|[01]?[0-9]):([0-5][0-9])").matcher(s);
        if (full.find()) {
            return two(full.group(1)) + "-" + two(full.group(2)) + " " + two(full.group(3)) + ":" + full.group(4);
        }

        Matcher md = Pattern.compile("(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])\\s+(20|21|22|23|[01]?[0-9]):([0-5][0-9])").matcher(s);
        if (md.find()) {
            return two(md.group(1)) + "-" + two(md.group(2)) + " " + two(md.group(3)) + ":" + md.group(4);
        }

        return "";
    }

    private String two(String n) {
        try {
            int v = Integer.parseInt(n);
            return v < 10 ? "0" + v : String.valueOf(v);
        } catch (Exception e) {
            return n;
        }
    }

    private String[] splitVisual(String text, int maxChars, int maxLines) {
        String s = safe(text, "").replaceAll("\\s+", " ").trim();
        if (s.length() <= maxChars) return new String[]{s};

        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < s.length() && lines.size() < maxLines) {
            int end = Math.min(s.length(), start + maxChars);
            if (end < s.length()) {
                int space = s.lastIndexOf(' ', end);
                if (space > start + 4) end = space;
            }
            String part = s.substring(start, end).trim();
            if (lines.size() == maxLines - 1 && end < s.length()) {
                part = limit(part + s.substring(end).trim(), maxChars + 1);
            }
            lines.add(part);
            start = end;
            while (start < s.length() && s.charAt(start) == ' ') start++;
        }
        return lines.toArray(new String[0]);
    }

    private String signed(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }

    private String limit(String s, int max) {
        if (s == null) return "";
        String v = s.trim();
        if (v.length() <= max) return v;
        return v.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String plainText(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", "")
                .trim();
    }

    private void ensureDir(String dir) {
        File f = new File(dir);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    private String e(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String json(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String read(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    private String readQuietly(InputStream in) {
        try {
            if (in == null) return "";
            return read(in);
        } catch (Exception e) {
            return "";
        }
    }

    private String extract(String text, String start, String end) {
        if (text == null) return "";
        int s = text.indexOf(start);
        if (s < 0) return "";
        s += start.length();
        int e = text.indexOf(end, s);
        if (e < 0) return "";
        return text.substring(s, e);
    }

    private String safe(String v, String d) {
        if (v == null || v.trim().isEmpty()) return d;
        return v.trim();
    }

    private String authorName(String author) {
        if ("laozhou".equals(author)) return "老周";
        if ("akai".equals(author)) return "阿凯";
        if ("laotang".equals(author)) return "老唐";
        if ("xiaobei".equals(author)) return "小北";
        return "顶红体育";
    }

    static class PublishState {
        Long id;
        String status;
        String draftMediaId;
        String publishId;
        String articleUrl;
    }

    static class Article {
        Long id;
        Long matchId;
        String title;
        String author;
        String category;
        String wechatTitle;
        String wechatSummary;
        String coverText;
        String coverHeadline;
        String matchTime;
        String sportType;
        String leagueName;
        String homeTeam;
        String awayTeam;
        String content;
        String status;
    }
}
