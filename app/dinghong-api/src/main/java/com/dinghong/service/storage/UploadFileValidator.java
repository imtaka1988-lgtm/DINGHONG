package com.dinghong.service.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 上传文件校验器。
 * 从 UploadController 中提取出来的独立性校验逻辑，便于复用和单独测试。
 */
@Component
public class UploadFileValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "webp", "gif"
    ));
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 校验文件，返回 null 表示通过，否则返回错误信息字符串。
     */
    public String validate(MultipartFile file) {
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
}
