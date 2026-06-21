package com.dinghong.service.wechat;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 微信客服消息发送服务。
 * 替代 WechatController.sendCustomerMessage() 中的硬编码。
 */
@Service
public class WechatMessageService {

    /**
     * 发送客服消息 JSON。
     * @param accessToken 微信 access_token
     * @param jsonMessage 消息体 JSON
     */
    public void sendCustomMessage(String accessToken, String jsonMessage) {
        try {
            String api = "https://api.weixin.qq.com/cgi-bin/message/custom/send"
                    + "?access_token=" + accessToken;

            HttpURLConnection conn = (HttpURLConnection) new URL(api).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonMessage.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    code == 200 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            System.out.println("[WECHAT_MSG] code=" + code + " resultLen=" + sb.length());

        } catch (Exception e) {
            System.out.println("[WECHAT_MSG_ERROR] " + e.getMessage());
        }
    }
}
