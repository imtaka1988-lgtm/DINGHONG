package com.dinghong.controller.admin;

import com.dinghong.repository.MatchLiveRepository;
import com.dinghong.service.storage.UploadFileValidator;
import com.dinghong.service.storage.UploadStorageService;
import com.dinghong.service.wechat.WechatAccessTokenService;
import com.dinghong.service.wechat.WechatMediaService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
public class UploadController {

    private final UploadStorageService storage;
    private final UploadFileValidator validator;
    private final MatchLiveRepository matchLiveRepo;
    private final WechatAccessTokenService accessTokenService;
    private final WechatMediaService mediaService;

    public UploadController(UploadStorageService storage,
                            UploadFileValidator validator,
                            MatchLiveRepository matchLiveRepo,
                            WechatAccessTokenService accessTokenService,
                            WechatMediaService mediaService) {
        this.storage = storage;
        this.validator = validator;
        this.matchLiveRepo = matchLiveRepo;
        this.accessTokenService = accessTokenService;
        this.mediaService = mediaService;
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
            matchLiveRepo.updateQr(matchId, imageUrl, mediaId);

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
