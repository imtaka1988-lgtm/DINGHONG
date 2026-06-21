package com.dinghong.repository;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * wechat_greeting_config / user_daily_greeting 表的数据访问层。
 * 替代 WechatController 中直接 DataSource.getConnection() 写 SQL 的模式。
 */
@Repository
public class WechatGreetingRepository {

    private final DataSource dataSource;

    public WechatGreetingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 读取欢迎语配置。
     */
    public GreetingConfigRow getConfig() throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT qr_image_url, greeting_text, enabled FROM wechat_greeting_config WHERE id=1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    GreetingConfigRow row = new GreetingConfigRow();
                    row.qrImageUrl = nvl(rs.getString("qr_image_url"));
                    row.greetingText = nvl(rs.getString("greeting_text"));
                    row.enabled = rs.getInt("enabled") == 1;
                    return row;
                }
            }
        }
        return null;
    }

    /**
     * 检查今天是否已发送过欢迎语。
     */
    public boolean isSentToday(String openid) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM user_daily_greeting WHERE openid=? AND greeting_date=CURDATE() LIMIT 1")) {
            ps.setString(1, openid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 记录今日已发送。
     */
    public void recordSent(String openid) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO user_daily_greeting (openid, greeting_date) VALUES (?, CURDATE())")) {
            ps.setString(1, openid);
            ps.executeUpdate();
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    public static class GreetingConfigRow {
        public String qrImageUrl;
        public String greetingText;
        public boolean enabled;
    }
}
