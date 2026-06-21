package com.dinghong.controller.admin;

import com.dinghong.service.wechat.WechatAccessTokenService;
import com.dinghong.service.wechat.WechatMediaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.*;
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
    private final WechatAccessTokenService accessTokenService;
    private final WechatMediaService mediaService;
    private final DataSource dataSource;

    public UploadController(@Value("${upload.dir}") String uploadDir,
                            @Value("${upload.public-base-url}") String publicBaseUrl,
                            WechatAccessTokenService accessTokenService,
                            WechatMediaService mediaService,
                            DataSource dataSource) {
        this.uploadDir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
        this.accessTokenService = accessTokenService;
        this.mediaService = mediaService;
        this.dataSource = dataSource;

        if (!this.publicBaseUrl.startsWith("http://") && !this.publicBaseUrl.startsWith("https://")) {
            throw new IllegalStateException(
                "UPLOAD_PUBLIC_BASE_URL 必须为 http:// 或 https:// 开头，当前值: " + this.publicBaseUrl
            );
        }

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

            String accessToken = accessTokenService.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                return imageUrl + "|(wechat token unavailable, media_id not uploaded)";
            }

            String mediaId = mediaService.uploadImage(accessToken, dest);

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

        return null;
    }
}
