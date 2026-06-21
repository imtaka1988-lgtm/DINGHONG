package com.dinghong.controller.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
public class UploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "webp", "gif"
    ));
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final String uploadDir;
    private final String publicBaseUrl;
    private final String wechatAppId;
    private final String wechatSecret;
    private final DataSource dataSource;

    public UploadController(@Value("${upload.dir}") String uploadDir,
                            @Value("${upload.public-base-url}") String publicBaseUrl,
                            @Value("${wechat.appid}") String wechatAppId,
                            @Value("${wechat.secret}") String wechatSecret,
                            DataSource dataSource) {
        this.uploadDir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
        this.wechatAppId = wechatAppId == null ? "" : wechatAppId.trim();
        this.wechatSecret = wechatSecret == null ? "" : wechatSecret.trim();
        this.dataSource = dataSource;

        // 校验 publicBaseUrl
        if (!this.publicBaseUrl.startsWith("http://") && !this.publicBaseUrl.startsWith("https://")) {
            throw new IllegalStateException(
                "UPLOAD_PUBLIC_BASE_URL 必须为 http:// 或 https:// 开头，当前值: " + this.publicBaseUrl
            );
        }

        // 确保上传目录存在
        File dir = new File(this.uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException(
                "无法创建上传目录: " + this.uploadDir + "。请检查 UPLOAD_DIR 配置和文件权限。"
            );
        }
    }

    @PostMapping("/admin/upload/{matchId}")
    public String uploadForMatch(@PathVariable Long matchId, @RequestParam("file") MultipartFile file) {
        String validated = validateFile(file);
        if (validated != null) return validated;

        try {
            String original = file.getOriginalFilename();
            String suffix = original.substring(original.lastIndexOf("."));
            String filename = UUID.randomUUID().toString().replace("-", "") + suffix;

            File dest = new File(uploadDir + filename);
            file.transferTo(dest);

            String imageUrl = publicBaseUrl + filename;

            String accessToken = getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                return imageUrl + "|(wechat token unavailable, media_id not uploaded)";
            }

            String mediaId = uploadToWechat(accessToken, dest);

            try (Connection conn = dataSource.getConnection()) {
                String sql = "UPDATE match_live SET qrcode_url=?, wechat_media_id=? WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, imageUrl);
                ps.setString(2, mediaId);
                ps.setLong(3, matchId);
                ps.executeUpdate();
            }

            return imageUrl + "|" + mediaId;

        } catch (Exception e) {
            return "upload error: " + e.getMessage();
        }
    }

    @PostMapping("/admin/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        String validated = validateFile(file);
        if (validated != null) return validated;

        try {
            String original = file.getOriginalFilename();
            String suffix = original.substring(original.lastIndexOf("."));
            String filename = UUID.randomUUID().toString().replace("-", "") + suffix;

            File dest = new File(uploadDir + filename);
            file.transferTo(dest);

            return publicBaseUrl + filename;

        } catch (Exception e) {
            return "upload error: " + e.getMessage();
        }
    }

    private String validateFile(MultipartFile file) {
        if (file.isEmpty()) return "file empty";

        String original = file.getOriginalFilename();
        if (original == null || original.isEmpty()) return "filename missing";
        if (file.getSize() > MAX_FILE_SIZE) return "file too large, max 10MB";

        int dot = original.lastIndexOf(".");
        if (dot == -1) return "file has no extension";

        String ext = original.substring(dot + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) return "extension not allowed: ." + ext;

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return "content type must be image/*, got: " + contentType;
        }

        return null; // valid
    }

    private String getAccessToken() {
        try {
            if (wechatAppId.isEmpty() || wechatSecret.isEmpty()) {
                System.out.println("[UPLOAD] WECHAT_APPID or WECHAT_SECRET not set, skip wechat upload");
                return null;
            }

            String api = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                    + wechatAppId + "&secret=" + wechatSecret;

            String json = httpGet(api);
            return extract(json, "access_token");
        } catch (Exception e) {
            System.out.println("[UPLOAD_TOKEN_ERROR] " + e.getMessage());
            return null;
        }
    }

    private String uploadToWechat(String token, File file) throws Exception {
        String boundary = "----DINGHONG" + System.currentTimeMillis();
        URL url = new URL("https://api.weixin.qq.com/cgi-bin/media/upload?access_token=" + token + "&type=image");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = conn.getOutputStream();
             FileInputStream fis = new FileInputStream(file)) {

            out.write(("--" + boundary + "\r\n").getBytes());
            out.write(("Content-Disposition: form-data; name=\"media\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
            out.write(("Content-Type: image/jpeg\r\n\r\n").getBytes());

            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            out.write(("\r\n--" + boundary + "--\r\n").getBytes());
        }

        String json = read(conn.getInputStream());
        return extract(json, "media_id");
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        return read(conn.getInputStream());
    }

    private String read(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private String extract(String json, String key) {
        String mark = "\"" + key + "\":\"";
        int s = json.indexOf(mark);
        if (s == -1) return "";
        int start = s + mark.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
