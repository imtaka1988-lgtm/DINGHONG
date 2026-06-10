package com.dinghong.controller.wechat;

import com.dinghong.service.MatchDbService;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class WechatController {

    private static final String TOKEN = "dinghong2026";

    private final MatchDbService matchService;
    private final DataSource dataSource;

    public WechatController(MatchDbService matchService, DataSource dataSource) {
        this.matchService = matchService;
        this.dataSource = dataSource;
    }

    @GetMapping("/wechat/callback")
    public String verify(String signature, String timestamp, String nonce, String echostr) {
        if (signature == null || timestamp == null || nonce == null || echostr == null) {
            return "wechat callback ok";
        }

        String[] arr = {TOKEN, timestamp, nonce};
        Arrays.sort(arr);
        String raw = String.join("", arr);
        String sha1 = sha1(raw);

        return sha1.equals(signature) ? echostr : "error";
    }

    @PostMapping(value = "/wechat/callback", produces = "application/xml;charset=UTF-8")
    public String receive(@RequestBody String xml) {
        System.out.println("[WECHAT_CALLBACK_XML] " + xml);

        String fromUser = getValue(xml, "FromUserName");
        String toUser = getValue(xml, "ToUserName");
        String msgType = getValue(xml, "MsgType");
        String content = getValue(xml, "Content");

        System.out.println("[WECHAT_CALLBACK_PARSED] from=" + fromUser + ", to=" + toUser + ", msgType=" + msgType + ", content=" + content);

        // 每日首次互动欢迎语
        trySendDailyGreeting(fromUser);

        if ("event".equalsIgnoreCase(msgType)) {
            String event = getValue(xml, "Event");
            String eventKey = getValue(xml, "EventKey");

            System.out.println("[WECHAT_EVENT] event=" + event + ", eventKey=" + eventKey);

            if ("subscribe".equalsIgnoreCase(event)) {
                String welcome = getSubscribeWelcome();
                return textXml(fromUser, toUser, welcome);
            }

            if ("CLICK".equalsIgnoreCase(event)) {
                content = eventKeyToContent(eventKey);
                System.out.println("[WECHAT_CLICK_TO_CONTENT] " + content);
            }
        }

        if (content == null || content.trim().isEmpty()) {
            content = "智能客服";
        }



        // 内容菜单唯一入口：今日推荐 / 昨日复盘 / 最新文章
        // 今日推荐：只展示今天发布/生成的赛前文章；没有就明确提示，不拿昨天兜底。
        // 昨日复盘：展示今天创作的复盘文章，用于复盘昨天预测；不是取昨天发布的复盘。
        // 最新文章：按真实发布时间倒序取最新 3 篇。
        if ("今日推荐".equals(content) || "昨日复盘".equals(content) || "最新文章".equals(content)) {
            java.util.List<java.util.Map<String, String>> articleNews = matchService.articleNewsItemsByMenu(content, 3);
            if (articleNews != null && !articleNews.isEmpty()) {
                return newsXml(fromUser, toUser, articleNews);
            }
            return textXml(fromUser, toUser, matchService.articleMenuEmptyText(content));
        }

        // 关键词 / 直播编号优先返回二维码海报，不再弹比赛图文名片
        String mediaId = matchService.getImageMediaId(content);

        if (mediaId != null && !mediaId.trim().isEmpty()) {
            System.out.println("[WECHAT_REPLY_IMAGE] mediaId=" + mediaId);
            return imageXml(fromUser, toUser, mediaId);
        }

        String reply = matchService.reply(content);
        System.out.println("[WECHAT_REPLY_TEXT] " + reply);

        return textXml(fromUser, toUser, reply);
    }

    private String eventKeyToContent(String key) {
        if ("RECENT_LIVE".equals(key)) return "最近直播";
        if ("TODAY_FOOTBALL".equals(key)) return "今日足球";
        if ("TODAY_BASKETBALL".equals(key)) return "今日篮球";

        if ("TODAY_PREVIEW".equals(key)) return "今日推荐";
        if ("YESTERDAY_REVIEW".equals(key)) return "昨日复盘";
        if ("LATEST_ARTICLE".equals(key)) return "最新文章";

        if ("AI_SERVICE".equals(key)) return "智能客服";
        if ("CONTACT".equals(key)) return "联系人工";

        return key == null ? "" : key;
    }


    private String newsXml(String to, String from, java.util.List<java.util.Map<String, String>> items) {
        int count = Math.min(items == null ? 0 : items.size(), 8);

        StringBuilder sb = new StringBuilder();
        sb.append("<xml>")
                .append("<ToUserName><![CDATA[").append(to).append("]]></ToUserName>")
                .append("<FromUserName><![CDATA[").append(from).append("]]></FromUserName>")
                .append("<CreateTime>").append(System.currentTimeMillis() / 1000).append("</CreateTime>")
                .append("<MsgType><![CDATA[news]]></MsgType>")
                .append("<ArticleCount>").append(count).append("</ArticleCount>")
                .append("<Articles>");

        for (int i = 0; i < count; i++) {
            java.util.Map<String, String> item = items.get(i);

            sb.append("<item>")
                    .append("<Title><![CDATA[").append(safe(item.get("title"))).append("]]></Title>")
                    .append("<Description><![CDATA[").append(safe(item.get("description"))).append("]]></Description>")
                    .append("<PicUrl><![CDATA[").append(safe(item.get("picUrl"))).append("]]></PicUrl>")
                    .append("<Url><![CDATA[").append(safe(item.get("url"))).append("]]></Url>")
                    .append("</item>");
        }

        sb.append("</Articles>")
                .append("</xml>");

        return sb.toString();
    }


    private String textXml(String to, String from, String content) {
        return "<xml>"
                + "<ToUserName><![CDATA[" + to + "]]></ToUserName>"
                + "<FromUserName><![CDATA[" + from + "]]></FromUserName>"
                + "<CreateTime>" + (System.currentTimeMillis() / 1000) + "</CreateTime>"
                + "<MsgType><![CDATA[text]]></MsgType>"
                + "<Content><![CDATA[" + safe(content) + "]]></Content>"
                + "</xml>";
    }

    private String imageXml(String to, String from, String mediaId) {
        return "<xml>"
                + "<ToUserName><![CDATA[" + to + "]]></ToUserName>"
                + "<FromUserName><![CDATA[" + from + "]]></FromUserName>"
                + "<CreateTime>" + (System.currentTimeMillis() / 1000) + "</CreateTime>"
                + "<MsgType><![CDATA[image]]></MsgType>"
                + "<Image><MediaId><![CDATA[" + mediaId + "]]></MediaId></Image>"
                + "</xml>";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String getValue(String xml, String tag) {
        if (xml == null || tag == null) return "";

        Pattern cdataPattern = Pattern.compile("<" + tag + ">\\s*<!\\[CDATA\\[(.*?)]]>\\s*</" + tag + ">", Pattern.DOTALL);
        Matcher cdataMatcher = cdataPattern.matcher(xml);
        if (cdataMatcher.find()) {
            return cdataMatcher.group(1).trim();
        }

        Pattern normalPattern = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL);
        Matcher normalMatcher = normalPattern.matcher(xml);
        if (normalMatcher.find()) {
            return normalMatcher.group(1).trim();
        }

        return "";
    }

    private String sha1(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();

            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ===================== 每日首次互动欢迎语 =====================

    /**
     * 检查用户今天是否首次互动，如果是则通过客服消息推送欢迎语（图片+文字）
     */
    private void trySendDailyGreeting(String openid) {
        if (openid == null || openid.trim().isEmpty()) return;
        openid = openid.trim();

        // 1. 检查功能是否启用
        GreetingConfig config = getGreetingConfig();
        if (config == null || !config.enabled) {
            System.out.println("[DAILY_GREETING] disabled, skip openid=" + openid);
            return;
        }

        // 2. 检查今天是否已发送过
        if (isGreetingSentToday(openid)) {
            System.out.println("[DAILY_GREETING] already sent today, skip openid=" + openid);
            return;
        }

        // 3. 记录今日已发送（先记再发，防止重复）
        recordGreetingSent(openid);

        // 4. 通过客服消息推送：先发图片，再发文字
        final String finalOpenid = openid;
        final GreetingConfig finalConfig = config;
        new Thread(() -> sendDailyGreeting(finalOpenid, finalConfig)).start();

        System.out.println("[DAILY_GREETING] triggered openid=" + finalOpenid + " qrUrl=" + finalConfig.qrImageUrl);
    }

    private static class GreetingConfig {
        String qrImageUrl;
        String greetingText;
        boolean enabled;
    }

    private GreetingConfig getGreetingConfig() {
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement(
                "SELECT qr_image_url, greeting_text, enabled FROM wechat_greeting_config WHERE id=1"
            );
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                GreetingConfig c = new GreetingConfig();
                c.qrImageUrl = safe(rs.getString("qr_image_url"));
                c.greetingText = safe(rs.getString("greeting_text"));
                c.enabled = rs.getInt("enabled") == 1;
                return c;
            }
        } catch (Exception e) {
            System.out.println("[DAILY_GREETING_GET_CONFIG_ERROR] " + e.getMessage());
        }
        return null;
    }

    private boolean isGreetingSentToday(String openid) {
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM user_daily_greeting WHERE openid=? AND greeting_date=CURDATE() LIMIT 1"
            );
            ps.setString(1, openid);
            java.sql.ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            System.out.println("[DAILY_GREETING_CHECK_ERROR] " + e.getMessage());
            return false;
        }
    }

    private void recordGreetingSent(String openid) {
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO user_daily_greeting (openid, greeting_date) VALUES (?, CURDATE())"
            );
            ps.setString(1, openid);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("[DAILY_GREETING_RECORD_ERROR] " + e.getMessage());
        }
    }

    /**
     * 通过微信客服消息接口发送欢迎语
     * 先发图片（二维码），再发文字
     */
    private void sendDailyGreeting(String openid, GreetingConfig config) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                System.out.println("[DAILY_GREETING] no access token, skip");
                return;
            }

            // 1. 先发送文字
            if (config.greetingText != null && !config.greetingText.isEmpty()) {
                String textJson = "{\"touser\":\"" + openid + "\",\"msgtype\":\"text\",\"text\":{\"content\":\"" + escapeJson(config.greetingText) + "\"}}";
                sendCustomerMessage(accessToken, textJson);
                Thread.sleep(300);
            }

            // 2. 再发送图片（如果配置了二维码URL）
            if (config.qrImageUrl != null && !config.qrImageUrl.isEmpty()) {
                String imageJson = "{\"touser\":\"" + openid + "\",\"msgtype\":\"image\",\"image\":{\"media_id\":\"" + getMediaIdByUrl(accessToken, config.qrImageUrl) + "\"}}";
                sendCustomerMessage(accessToken, imageJson);
            }

        } catch (Exception e) {
            System.out.println("[DAILY_GREETING_SEND_ERROR] " + e.getMessage());
        }
    }

    private void sendCustomerMessage(String accessToken, String json) {
        try {
            String api = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=" + accessToken;
            HttpURLConnection conn = (HttpURLConnection) new URL(api).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    code == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            System.out.println("[DAILY_GREETING_CUSTOM_MSG] code=" + code + " resp=" + sb);
        } catch (Exception e) {
            System.out.println("[DAILY_GREETING_CUSTOM_MSG_ERROR] " + e.getMessage());
        }
    }

    private String getMediaIdByUrl(String accessToken, String imageUrl) {
        // 如果 qrImageUrl 本身就是微信 media_id（不是 http 开头），直接返回
        if (imageUrl != null && !imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            System.out.println("[DAILY_GREETING] using direct media_id=" + imageUrl);
            return imageUrl;
        }

        // 否则作为外部图片上传到微信临时素材
        try {
            String uploadApi = "https://api.weixin.qq.com/cgi-bin/media/upload?access_token=" + accessToken + "&type=image";

            // 下载图片
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            HttpURLConnection imgConn = (HttpURLConnection) new URL(imageUrl).openConnection();
            imgConn.setRequestMethod("GET");
            imgConn.setConnectTimeout(8000);
            imgConn.setReadTimeout(8000);
            imgConn.setRequestProperty("User-Agent", "DingHong/1.0");

            try (java.io.InputStream is = imgConn.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
            }

            byte[] imageBytes = baos.toByteArray();

            // 上传到微信
            HttpURLConnection uploadConn = (HttpURLConnection) new URL(uploadApi).openConnection();
            uploadConn.setRequestMethod("POST");
            uploadConn.setDoOutput(true);
            uploadConn.setConnectTimeout(10000);
            uploadConn.setReadTimeout(10000);

            String boundary = "----DingHongFormBoundary" + System.currentTimeMillis();
            uploadConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = uploadConn.getOutputStream()) {
                os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(("Content-Disposition: form-data; name=\"media\"; filename=\"qr.jpg\"\r\n").getBytes(StandardCharsets.UTF_8));
                os.write("Content-Type: image/jpeg\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                os.write(imageBytes);
                os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }

            int code = uploadConn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    code == 200 ? uploadConn.getInputStream() : uploadConn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            String resp = sb.toString();
            System.out.println("[DAILY_GREETING_UPLOAD] code=" + code + " resp=" + resp);

            // 提取 media_id
            String mediaId = extractJsonString(resp, "media_id");
            return mediaId != null ? mediaId : "";

        } catch (Exception e) {
            System.out.println("[DAILY_GREETING_UPLOAD_ERROR] " + e.getMessage());
            return "";
        }
    }

    private String getAccessToken() {
        try {
            String appId = System.getenv("WECHAT_APPID");
            String secret = System.getenv("WECHAT_SECRET");
            if (appId == null || secret == null || appId.isEmpty() || secret.isEmpty()) {
                System.out.println("[DAILY_GREETING] WECHAT_APPID or WECHAT_SECRET not set");
                return null;
            }

            String tokenUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + secret;
            HttpURLConnection conn = (HttpURLConnection) new URL(tokenUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            String json = sb.toString();
            System.out.println("[DAILY_GREETING_TOKEN] resp=" + json);

            return extractJsonString(json, "access_token");
        } catch (Exception e) {
            System.out.println("[DAILY_GREETING_TOKEN_ERROR] " + e.getMessage());
            return null;
        }
    }

    private String extractJsonString(String json, String key) {
        if (json == null || key == null) return null;
        String prefix = "\"" + key + "\":\"";
        int start = json.indexOf(prefix);
        if (start == -1) return null;
        start += prefix.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    // ===================== 每日首次互动欢迎语 END =====================

    // ===================== 关注默认欢迎语 =====================

    /**
     * 关注欢迎语：优先使用后台 wechat_greeting_config 中启用的配置，
     * 如果未配置或未启用，则使用硬编码的默认欢迎文案。
     */
    private String getSubscribeWelcome() {
        // 1. 尝试从数据库读取后台配置
        GreetingConfig config = getGreetingConfig();

        if (config != null && config.enabled
                && config.greetingText != null && !config.greetingText.trim().isEmpty()) {
            System.out.println("[SUBSCRIBE_WELCOME] using configured greeting from DB");
            StringBuilder sb = new StringBuilder();
            sb.append(config.greetingText.trim());
            if (config.qrImageUrl != null && !config.qrImageUrl.trim().isEmpty()) {
                sb.append("\n\n").append("群二维码：").append(config.qrImageUrl.trim());
            }
            return sb.toString();
        }

        // 2. 没有后台配置或已禁用，使用默认文案
        System.out.println("[SUBSCRIBE_WELCOME] using default greeting");
        return "感谢关注顶红体育。\n"
                + "热门菜单\n"
                + "①每日推荐：四位足篮顶级编辑的红\ud83d\udd34推荐！\n"
                + "②昨日复盘：总编根据总结昨日的赛事和推荐进行复盘！\n"
                + "③赛事直播：只需点击菜单即可弹出二维码扫码观看！\n"
                + "④隐藏新版块：高清免费体育赛事直播站正在搭建中......敬请期待......";
    }

    // ===================== 关注默认欢迎语 END =====================
}
