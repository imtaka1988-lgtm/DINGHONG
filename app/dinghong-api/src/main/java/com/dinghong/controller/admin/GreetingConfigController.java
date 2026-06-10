package com.dinghong.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@RestController
@RequestMapping("/admin")
public class GreetingConfigController {

    private final DataSource dataSource;

    public GreetingConfigController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/wechat-greeting")
    public ObjectNode getConfig() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, qr_image_url, greeting_text, enabled FROM wechat_greeting_config WHERE id=1"
            );
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result.put("id", rs.getLong("id"));
                result.put("qrImageUrl", safe(rs.getString("qr_image_url")));
                result.put("greetingText", safe(rs.getString("greeting_text")));
                result.put("enabled", rs.getInt("enabled") == 1);
            } else {
                result.put("id", 0);
                result.put("qrImageUrl", "");
                result.put("greetingText", "欢迎加入顶红体育交流群，扫码入群领取每日精选赛事分析");
                result.put("enabled", true);
            }
        } catch (Exception e) {
            System.out.println("[GREETING_CONFIG_GET_ERROR] " + e.getMessage());
        }

        return result;
    }

    @PutMapping("/wechat-greeting")
    public ObjectNode updateConfig(@RequestBody JsonNode body) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();

        try {
            String qrImageUrl = body.has("qrImageUrl") ? body.get("qrImageUrl").asText("").trim() : "";
            String greetingText = body.has("greetingText") ? body.get("greetingText").asText("").trim() : "";
            boolean enabled = !body.has("enabled") || body.get("enabled").asBoolean(true);

            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO wechat_greeting_config (id, qr_image_url, greeting_text, enabled) VALUES (1, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE qr_image_url=?, greeting_text=?, enabled=?"
                );
                ps.setString(1, qrImageUrl);
                ps.setString(2, greetingText);
                ps.setInt(3, enabled ? 1 : 0);
                ps.setString(4, qrImageUrl);
                ps.setString(5, greetingText);
                ps.setInt(6, enabled ? 1 : 0);
                ps.executeUpdate();

                result.put("success", true);
                result.put("message", "保存成功");
                System.out.println("[GREETING_CONFIG_SAVE] qrImageUrl=" + qrImageUrl + " greetingText=" + greetingText + " enabled=" + enabled);
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
            System.out.println("[GREETING_CONFIG_SAVE_ERROR] " + e.getMessage());
        }

        return result;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}