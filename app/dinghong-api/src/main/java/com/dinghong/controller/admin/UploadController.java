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
import java.util.UUID;

@RestController
public class UploadController {

    private final String uploadDir;
    private final String publicBaseUrl;
    private final DataSource dataSource;

    public UploadController(@Value("${upload.dir}") String uploadDir,
                            @Value("${upload.public-base-url}") String publicBaseUrl,
                            DataSource dataSource) {
        this.uploadDir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
        this.dataSource = dataSource;

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
        try {
            if (file.isEmpty()) return "file empty";

            String original = file.getOriginalFilename();
            String suffix = original.substring(original.lastIndexOf("."));
            String filename = UUID.randomUUID().toString().replace("-", "") + suffix;

            File dest = new File(uploadDir + filename);
            file.transferTo(dest);

            String imageUrl = publicBaseUrl + filename;

            String accessToken = getAccessToken();
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
        try {
            if (file.isEmpty()) return "file empty";

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

    private String getAccessToken() throws Exception {
        String appid = System.getenv("WECHAT_APPID");
        String secret = System.getenv("WECHAT_SECRET");
        // Note: UploadController also uses System.getenv for WECHAT credentials
        // because it needs them at upload time, not at construction time.
        // This is acceptable as these env vars are set at server startup.

        String api = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + appid + "&secret=" + secret;

        String json = httpGet(api);
        return extract(json, "access_token");
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
