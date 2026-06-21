package com.dinghong.service.wechat;

import com.dinghong.config.WechatProperties;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 微信 access_token 统一获取服务。
 * 替代 WechatController / UploadController / LiveQrController 中 3 处重复的获取逻辑。
 */
@Service
public class WechatAccessTokenService {

    private final WechatProperties config;

    public WechatAccessTokenService(WechatProperties config) {
        this.config = config;
    }

    /**
     * 获取 access_token。如果 appid/secret 未配置则返回 null。
     */
    public String getAccessToken() {
        String appId = config.getAppid();
        String secret = config.getSecret();

        if (appId == null || appId.isEmpty() || secret == null || secret.isEmpty()) {
            System.out.println("[WECHAT_TOKEN] appid or secret not configured");
            return null;
        }

        try {
            String api = "https://api.weixin.qq.com/cgi-bin/token"
                    + "?grant_type=client_credential&appid=" + appId
                    + "&secret=" + secret;

            HttpURLConnection conn = (HttpURLConnection) new URL(api).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            String json = sb.toString();
            String token = extractValue(json, "access_token");
            System.out.println("[WECHAT_TOKEN] got token? " + (token != null));
            return token;

        } catch (Exception e) {
            System.out.println("[WECHAT_TOKEN_ERROR] " + e.getMessage());
            return null;
        }
    }

    private String extractValue(String json, String key) {
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
}
