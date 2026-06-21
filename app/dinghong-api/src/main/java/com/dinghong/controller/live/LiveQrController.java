package com.dinghong.controller.live;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.sql.DataSource;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/matches")
public class LiveQrController {

    private final String uploadDir;
    private final String publicPrefix;
    private final String playPrefix;
    private final DataSource dataSource;

    public LiveQrController(@Value("${upload.live-qr-dir}") String uploadDir,
                            @Value("${upload.live-qr-public-prefix}") String publicPrefix,
                            @Value("${upload.live-play-prefix}") String playPrefix,
                            DataSource dataSource) {
        this.uploadDir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        this.publicPrefix = publicPrefix.endsWith("/") ? publicPrefix : publicPrefix + "/";
        this.playPrefix = playPrefix;
        this.dataSource = dataSource;

        // 确保目录存在
        File dir = new File(this.uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException(
                "无法创建 Live QR 上传目录: " + this.uploadDir + "。请检查配置和文件权限。"
            );
        }
    }

    @PostMapping("/{id}/qr")
    public String generateQr(@PathVariable Long id) {
        try {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

            String streamKey;
            String homeTeam;
            String awayTeam;
            String matchTime;

            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, stream_key, home_team, away_team, match_time FROM match_live WHERE id=? LIMIT 1"
                );
                ps.setLong(1, id);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    return "error: 比赛不存在";
                }

                streamKey = safe(rs.getString("stream_key"));
                homeTeam = safe(rs.getString("home_team"));
                awayTeam = safe(rs.getString("away_team"));
                matchTime = safe(rs.getString("match_time"));

                if (streamKey.isEmpty()) {
                    streamKey = "live_" + id;
                    PreparedStatement ups = conn.prepareStatement(
                            "UPDATE match_live SET stream_key=? WHERE id=?"
                    );
                    ups.setString(1, streamKey);
                    ups.setLong(2, id);
                    ups.executeUpdate();
                }
            }

        String playUrl = playPrefix + streamKey;

            // 原始二维码文件（内部用）
            File rawQrFile = new File(uploadDir + streamKey + "_raw.png");
            // 最终公众号使用的海报图
            File posterFile = new File(uploadDir + streamKey + ".png");

            createQrPng(playUrl, rawQrFile);
            createPosterPng(rawQrFile, posterFile, homeTeam, awayTeam, matchTime);

        String imageUrl = publicPrefix + posterFile.getName();

            String accessToken = getAccessToken();
            String mediaId = uploadToWechat(accessToken, posterFile);

            if (mediaId == null || mediaId.trim().isEmpty()) {
                return "error: 二维码海报已生成，但上传微信素材失败。图片地址：" + imageUrl;
            }

            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE match_live SET qrcode_url=?, wechat_media_id=? WHERE id=?"
                );
                ps.setString(1, imageUrl);
                ps.setString(2, mediaId);
                ps.setLong(3, id);
                ps.executeUpdate();
            }

            return "ok|" + imageUrl + "|" + mediaId + "|" + playUrl;

        } catch (Exception e) {
            e.printStackTrace();
            return "error: " + e.getMessage();
        }
    }

    private void createQrPng(String text, File file) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 430, 430, hints);

        Path path = FileSystems.getDefault().getPath(file.getAbsolutePath());
        MatrixToImageWriter.writeToPath(matrix, "PNG", path);
    }

    private void createPosterPng(File qrFile, File posterFile, String homeTeam, String awayTeam, String matchTime) throws Exception {
        int width = 900;
        int height = 1280;

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 白底
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // 顶部标题：主队 VS 客队
        String title = buildTitle(homeTeam, awayTeam);
        g.setColor(new Color(20, 20, 20));
        g.setFont(posterFont(Font.BOLD, 46));
        drawCenteredText(g, title, width, 120);

        // 开赛时间：显示在队伍名下方
        String posterTime = formatPosterTime(matchTime);
        if (!posterTime.isEmpty()) {
            g.setColor(new Color(80, 80, 80));
            g.setFont(posterFont(Font.PLAIN, 28));
            drawCenteredText(g, "开赛时间：" + posterTime, width, 175);
        }

        // 中间提示文案
        g.setColor(new Color(55, 55, 55));
        g.setFont(posterFont(Font.PLAIN, 34));
        drawCenteredText(g, "保存图片到相册", width, 245);
        drawCenteredText(g, "用浏览器扫码打开", width, 300);

        // 二维码外框卡片
        int cardW = 560;
        int cardH = 560;
        int cardX = (width - cardW) / 2;
        int cardY = 350;

        g.setColor(new Color(246, 246, 246));
        g.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 36, 36));

        g.setColor(new Color(225, 225, 225));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 36, 36));

        BufferedImage qr = ImageIO.read(qrFile);
        int qrSize = 430;
        int qrX = (width - qrSize) / 2;
        int qrY = cardY + (cardH - qrSize) / 2;
        g.drawImage(qr, qrX, qrY, qrSize, qrSize, null);

        // 底部提示文案

        // 底部强提示：红色大字，提醒不要在微信内识别
        g.setColor(new Color(220, 38, 38));
        g.setFont(posterFont(Font.BOLD, 58));
        drawCenteredText(g, "微信内识别无效!!!", width, 1045);

        g.dispose();
        ImageIO.write(canvas, "png", posterFile);
    }


    private Font pickFont(int style, int size) {
        String[] preferred = {
                "WenQuanYi Micro Hei",
                "WenQuanYi Micro Hei Mono",
                "Droid Sans",
                "Noto Sans CJK SC",
                "Source Han Sans SC",
                "Microsoft YaHei",
                "SimHei",
                "SansSerif"
        };

        String[] available = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();

        for (String want : preferred) {
            for (String have : available) {
                if (want.equalsIgnoreCase(have)) {
                    return new Font(have, style, size);
                }
            }
        }

        return new Font("WenQuanYi Micro Hei", style, size);
    }


    private Font posterFont(int style, int size) {
        String testText = "主队客队请保存图片到相册用手机浏览器扫码打开";

        String[] fontFiles = {
                "/usr/share/fonts/google-droid/DroidSansFallback.ttf",
                "/usr/share/fonts/wqy-microhei/wqy-microhei.ttc"
        };

        for (String fontPath : fontFiles) {
            try {
                File f = new File(fontPath);
                if (!f.exists()) continue;

                Font base = Font.createFont(Font.TRUETYPE_FONT, f);
                Font font = base.deriveFont(style, (float) size);

                if (font.canDisplayUpTo(testText) == -1) {
                    System.out.println("[LIVE_QR_FONT] using font file: " + fontPath);
                    return font;
                } else {
                    System.out.println("[LIVE_QR_FONT] font cannot display all Chinese: " + fontPath);
                }
            } catch (Exception e) {
                System.out.println("[LIVE_QR_FONT_ERROR] " + fontPath + " => " + e.getMessage());
            }
        }

        String[] names = {
                "WenQuanYi Micro Hei",
                "WenQuanYi Micro Hei Mono",
                "Droid Sans",
                "Dialog",
                "SansSerif"
        };

        for (String name : names) {
            try {
                Font font = new Font(name, style, size);
                if (font.canDisplayUpTo(testText) == -1) {
                    System.out.println("[LIVE_QR_FONT] using font name: " + name);
                    return font;
                }
            } catch (Exception ignored) {}
        }

        System.out.println("[LIVE_QR_FONT] fallback to Dialog, Chinese may display incorrectly");
        return new Font("Dialog", style, size);
    }


    private String formatPosterTime(String matchTime) {
        String t = safe(matchTime);
        if (t.isEmpty()) return "";

        // 兼容：2026-06-03 20:00 / 06-03 20:00 / 6-3 20:00
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?:(\\d{4})[-/年])?(\\d{1,2})[-/月](\\d{1,2})[日\\s]+(\\d{1,2}:\\d{2})")
                    .matcher(t);

            if (m.find()) {
                int month = Integer.parseInt(m.group(2));
                int day = Integer.parseInt(m.group(3));
                String hm = m.group(4);
                return String.format("%02d月%02d日 %s", month, day, hm);
            }
        } catch (Exception ignored) {}

        // 如果本来就是中文格式，就直接显示
        return t;
    }

    private String buildTitle(String homeTeam, String awayTeam) {
        String home = safe(homeTeam);
        String away = safe(awayTeam);

        if (!home.isEmpty() && !away.isEmpty()) {
            return home + " VS " + away;
        }
        if (!home.isEmpty()) return home;
        if (!away.isEmpty()) return away;
        return "直播入口二维码";
    }

    private void drawCenteredText(Graphics2D g, String text, int width, int y) {
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = (width - textWidth) / 2;
        g.drawString(text, x, y);
    }

    private String getAccessToken() throws Exception {
        String appid = System.getenv("WECHAT_APPID");
        String secret = System.getenv("WECHAT_SECRET");

        String api = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + appid + "&secret=" + secret;

        String json = httpGet(api);
        return extract(json, "access_token");
    }

    private String uploadToWechat(String token, File file) throws Exception {
        String boundary = "----DINGHONGQR" + System.currentTimeMillis();
        URL url = new URL("https://api.weixin.qq.com/cgi-bin/media/upload?access_token=" + token + "&type=image");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = conn.getOutputStream();
             FileInputStream fis = new FileInputStream(file)) {

            out.write(("--" + boundary + "\r\n").getBytes());
            out.write(("Content-Disposition: form-data; name=\"media\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
            out.write(("Content-Type: image/png\r\n\r\n").getBytes());

            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            out.write(("\r\n--" + boundary + "--\r\n").getBytes());
        }

        InputStream in;
        if (conn.getResponseCode() >= 400) {
            in = conn.getErrorStream();
        } else {
            in = conn.getInputStream();
        }

        String json = read(in);
        return extract(json, "media_id");
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        InputStream in;
        if (conn.getResponseCode() >= 400) {
            in = conn.getErrorStream();
        } else {
            in = conn.getInputStream();
        }

        return read(in);
    }

    private String read(InputStream in) throws Exception {
        if (in == null) return "";

        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    private String extract(String json, String key) {
        if (json == null) return "";

        String mark = "\"" + key + "\":\"";
        int s = json.indexOf(mark);
        if (s == -1) return "";

        int start = s + mark.length();
        int end = json.indexOf("\"", start);

        if (end == -1) return "";

        return json.substring(start, end);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
