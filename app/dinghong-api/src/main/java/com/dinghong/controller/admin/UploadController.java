package com.dinghong.controller.admin;

import com.dinghong.service.storage.UploadFileValidator;
import com.dinghong.service.storage.UploadStorageService;
import com.dinghong.service.wechat.WechatAccessTokenService;
import com.dinghong.service.wechat.WechatMediaService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;

@RestController
public class UploadController {

    private final UploadStorageService storage;
    private final UploadFileValidator validator;
    private final WechatAccessTokenService accessTokenService;
    private final WechatMediaService mediaService;
    private final DataSource dataSource;

    public UploadController(UploadStorageService storage,
                            UploadFileValidator validator,
                            WechatAccessTokenService accessTokenService,
                            WechatMediaService mediaService,
                            DataSource dataSource) {
        this.storage = storage;
        this.validator = validator;
        this.accessTokenService = accessTokenService;
        this.mediaService = mediaService;
        this.dataSource = dataSource;
    }

    @PostMapping("/admin/upload/{matchId}")
    public String uploadForMatch(@PathVariable Long matchId, @RequestParam("file") MultipartFile file) {
        String validated = validator.validate(file);
        if (validated != null) return validated;

        try {
            File dest = storage.saveToFile(file);
            String imageUrl = storage.getPublicBaseUrl() + dest.getName();

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
        String validated = validator.validate(file);
        if (validated != null) return validated;

        try {
            return storage.saveUpload(file);
        } catch (Exception e) {
            return "upload error: " + e.getMessage();
        }
    }
}
