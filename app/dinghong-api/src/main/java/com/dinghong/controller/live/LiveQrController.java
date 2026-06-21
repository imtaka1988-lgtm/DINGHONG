package com.dinghong.controller.live;

import com.dinghong.repository.MatchLiveRepository;
import com.dinghong.service.storage.UploadStorageService;
import com.dinghong.service.wechat.WechatAccessTokenService;
import com.dinghong.service.wechat.WechatMediaService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/matches")
public class LiveQrController {

    private final MatchLiveRepository matchLiveRepo;
    private final UploadStorageService storage;
    private final WechatAccessTokenService accessTokenService;
    private final WechatMediaService mediaService;

    public LiveQrController(MatchLiveRepository matchLiveRepo,
                            UploadStorageService storage,
                            WechatAccessTokenService accessTokenService,
                            WechatMediaService mediaService) {
        this.matchLiveRepo = matchLiveRepo;
        this.storage = storage;
        this.accessTokenService = accessTokenService;
        this.mediaService = mediaService;
    }

    @PostMapping("/{id}/qr")
    public String generateQr(@PathVariable Long id) {
        try {
            MatchLiveRepository.MatchLiveRow match = matchLiveRepo.findById(id);
            if (match == null) {
                return "error: 比赛不存在";
            }

            String streamKey = matchLiveRepo.ensureStreamKey(id, match.streamKey);
            String playUrl = storage.getLivePlayPrefix() + streamKey;

            File rawQrFile = storage.getLiveQrRawFile(streamKey);
            File posterFile = storage.getLiveQrFile(streamKey);

            createQrPng(playUrl, rawQrFile);
            createPosterPng(rawQrFile, posterFile, match.homeTeam, match.awayTeam, match.matchTime);

            String imageUrl = storage.getLiveQrPublicPrefix() + posterFile.getName();

            String accessToken = getAccessToken();
            String mediaId = mediaService.uploadImagePng(accessToken, posterFile);

            if (mediaId == null || mediaId.trim().isEmpty()) {
                return "error: 二维码海报已生成，但上传微信素材失败。图片地址：" + imageUrl;
            }

            matchLiveRepo.updateQr(id, imageUrl, mediaId);

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

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        String title = buildTitle(homeTeam, awayTeam);
        g.setColor(new Color(20, 20, 20));
        g.setFont(posterFont(Font.BOLD, 46));
        drawCenteredText(g, title, width, 120);

        String posterTime = formatPosterTime(matchTime);
        if (!posterTime.isEmpty()) {
            g.setColor(new Color(80, 80, 80));
            g.setFont(posterFont(Font.PLAIN, 28));
            drawCenteredText(g, "开赛时间：" + posterTime, width, 175);
        }

        g.setColor(new Color(55, 55, 55));
        g.setFont(posterFont(Font.PLAIN, 34));
        drawCenteredText(g, "保存图片到相册", width, 245);
        drawCenteredText(g, "用浏览器扫码打开", width, 300);

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

        g.setColor(new Color(220, 38, 38));
        g.setFont(posterFont(Font.BOLD, 58));
        drawCenteredText(g, "微信内识别无效!!!", width, 1045);

        g.dispose();
        ImageIO.write(canvas, "png", posterFile);
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
                    return font;
                }
            } catch (Exception ignored) {
            }
        }
        return new Font("Dialog", style, size);
    }

    private String formatPosterTime(String matchTime) {
        String t = matchTime == null ? "" : matchTime.trim();
        if (t.isEmpty()) return "";
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
        } catch (Exception ignored) {
        }
        return t;
    }

    private String buildTitle(String homeTeam, String awayTeam) {
        String home = homeTeam == null ? "" : homeTeam.trim();
        String away = awayTeam == null ? "" : awayTeam.trim();
        if (!home.isEmpty() && !away.isEmpty()) return home + " VS " + away;
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
        String token = accessTokenService.getAccessToken();
        if (token == null || token.isEmpty()) {
            System.out.println("[LIVE_QR] WECHAT_APPID or WECHAT_SECRET not set, skip wechat upload");
            throw new RuntimeException("wechat credentials not configured");
        }
        return token;
    }
}
