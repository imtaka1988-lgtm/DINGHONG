package com.dinghong.service.wechat;

import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class WechatMenuService {

    public String createMenu() {
        try {
            String token = getAccessToken();
            if (empty(token)) return "error: access_token 获取失败，请检查 WECHAT_APPID / WECHAT_SECRET";

            String menuJson = "{"
                    + "\"button\":["
                    + "{"
                    + "\"name\":\"看直播\","
                    + "\"sub_button\":["
                    + "{\"type\":\"click\",\"name\":\"最近直播\",\"key\":\"RECENT_LIVE\"},"
                    + "{\"type\":\"click\",\"name\":\"今日足球\",\"key\":\"TODAY_FOOTBALL\"},"
                    + "{\"type\":\"click\",\"name\":\"今日篮球\",\"key\":\"TODAY_BASKETBALL\"}"
                    + "]"
                    + "},"
                    + "{"
                    + "\"name\":\"看内容\","
                    + "\"sub_button\":["
                    + "{\"type\":\"click\",\"name\":\"今日推荐\",\"key\":\"TODAY_PREVIEW\"},"
                    + "{\"type\":\"click\",\"name\":\"昨日复盘\",\"key\":\"YESTERDAY_REVIEW\"},"
                    + "{\"type\":\"click\",\"name\":\"最新文章\",\"key\":\"LATEST_ARTICLE\"}"
                    + "]"
                    + "},"
                    + "{"
                    + "\"name\":\"问客服\","
                    + "\"sub_button\":["
                    + "{\"type\":\"click\",\"name\":\"智能客服\",\"key\":\"AI_SERVICE\"},"
                    + "{\"type\":\"click\",\"name\":\"联系人工\",\"key\":\"CONTACT\"}"
                    + "]"
                    + "}"
                    + "]"
                    + "}";

            return post("https://api.weixin.qq.com/cgi-bin/menu/create?access_token=" + token, menuJson);

        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    public String getMenu() {
        try {
            String token = getAccessToken();
            if (empty(token)) return "error: access_token 获取失败，请检查 WECHAT_APPID / WECHAT_SECRET";
            return get("https://api.weixin.qq.com/cgi-bin/menu/get?access_token=" + token);
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    public String deleteMenu() {
        try {
            String token = getAccessToken();
            if (empty(token)) return "error: access_token 获取失败，请检查 WECHAT_APPID / WECHAT_SECRET";
            return get("https://api.weixin.qq.com/cgi-bin/menu/delete?access_token=" + token);
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    private String getAccessToken() throws Exception {
        String appid = System.getenv("WECHAT_APPID");
        String secret = System.getenv("WECHAT_SECRET");

        if (empty(appid) || empty(secret)) return "";

        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + appid + "&secret=" + secret;

        String json = get(url);
        String token = extract(json, "\"access_token\":\"", "\"");

        if (empty(token)) {
            System.out.println("[WECHAT_TOKEN_ERROR] " + json);
        }

        return token;
    }

    private String post(String urlStr, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }

        return read(conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream());
    }

    private String get(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        return read(conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream());
    }

    private String extract(String text, String start, String end) {
        if (text == null) return "";
        int s = text.indexOf(start);
        if (s < 0) return "";
        int e = text.indexOf(end, s + start.length());
        if (e < 0) return "";
        return text.substring(s + start.length(), e);
    }

    private String read(InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private boolean empty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
