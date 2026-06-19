package com.dinghong.controller.live;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HexFormat;

@RestController
@RequestMapping("/live")
@CrossOrigin(origins = "*")
public class LiveProxyController {

    private final DataSource dataSource;
    // 代理签名密钥，生产环境应通过环境变量注入
    private static final String PROXY_SECRET = "dinghong-proxy-secret-2026";

    public LiveProxyController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/proxy")
    public ResponseEntity<StreamingResponseBody> proxy(@RequestParam String key,
                                                        @RequestParam(required = false) Long t,
                                                        @RequestParam(required = false) String s) {
        // 时效性校验：请求必须在5分钟内
        if (t == null || s == null) {
            return text(403, "proxy denied: missing auth params");
        }

        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - t) > 300) {
            return text(403, "proxy denied: token expired");
        }

        // HMAC 签名校验
        String expectedSig = hmacSha256(key + "|" + t, PROXY_SECRET);
        if (!expectedSig.equals(s)) {
            return text(403, "proxy denied: invalid signature");
        }

        try {
            LiveTarget target = findTarget(key);

            if (target == null || empty(target.url)) {
                return text(404, "live unavailable");
            }

            StreamingResponseBody body = output -> {
                HttpURLConnection conn = null;
                InputStream in = null;

                try {
                    URL url = new URL(target.url);
                    conn = (HttpURLConnection) url.openConnection();

                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(30000);
                    conn.setInstanceFollowRedirects(true);

                    conn.setRequestProperty("User-Agent", "VLC/3.0.20 LibVLC/3.0.20");
                    conn.setRequestProperty("Accept", "*/*");
                    conn.setRequestProperty("Connection", "keep-alive");

                    int code = conn.getResponseCode();
                    in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();

                    if (in == null) return;

                    byte[] buffer = new byte[8192];
                    int len;

                    while ((len = in.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                        output.flush();
                    }

                } catch (Exception ignored) {
                } finally {
                    try {
                        if (in != null) in.close();
                    } catch (Exception ignored) {}

                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "video/x-flv");
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Access-Control-Allow-Methods", "GET,OPTIONS");
            headers.set("Access-Control-Allow-Headers", "*");
            headers.set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            headers.set("Pragma", "no-cache");
            headers.set("X-Accel-Buffering", "no");

            return new ResponseEntity<>(body, headers, HttpStatus.OK);

        } catch (Exception e) {
            return text(500, "proxy error: " + e.getMessage());
        }
    }

    private LiveTarget findTarget(String key) throws Exception {
        if (empty(key)) return null;

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT stream_url, live_status, show_in_wechat " +
                            "FROM match_live WHERE stream_key=? LIMIT 1"
            );
            ps.setString(1, key.trim());

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            String url = safe(rs.getString("stream_url"));
            String status = safe(rs.getString("live_status"));
            int show = rs.getInt("show_in_wechat");

            if (show == 0) return null;
            if (empty(url)) return null;
            if (isOfflineStatus(status)) return null;

            return new LiveTarget(url);
        }
    }

    private boolean isOfflineStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        return s.equals("FINISHED")
                || s.equals("OFFLINE")
                || s.equals("ENDED")
                || s.equals("CLOSED")
                || s.equals("DELETE")
                || s.equals("DELETED")
                || s.equals("NO_RIGHTS");
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }

    private ResponseEntity<StreamingResponseBody> text(int status, String message) {
        StreamingResponseBody body = output -> output.write(message.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=UTF-8");
        headers.set("Access-Control-Allow-Origin", "*");

        return new ResponseEntity<>(body, headers, HttpStatus.valueOf(status));
    }

    private boolean empty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static class LiveTarget {
        private final String url;

        private LiveTarget(String url) {
            this.url = url;
        }
    }
}
