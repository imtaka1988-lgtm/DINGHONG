package com.dinghong.service.storage;

import com.dinghong.config.UploadProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.UUID;

/**
 * 统一文件存储服务。
 * 替代 UploadController / LiveQrController 中重复的路径拼接和文件保存逻辑。
 */
@Service
public class UploadStorageService {

    private final UploadProperties props;

    public UploadStorageService(UploadProperties props) {
        this.props = props;
        // 校验 publicBaseUrl
        String url = props.getPublicBaseUrl();
        if (url != null && !url.isEmpty()
                && !url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalStateException(
                "UPLOAD_PUBLIC_BASE_URL 必须以 http:// 或 https:// 开头，当前值: " + url
            );
        }
        // 校验必要目录不为空
        String dir = props.getDir();
        if (dir == null || dir.trim().isEmpty()) {
            throw new IllegalStateException("UPLOAD_DIR 不能为空。请在 .env 中设置 UPLOAD_DIR。");
        }
    }

    @PostConstruct
    public void init() {
        ensureDir(getMainDir());
        ensureDir(getLiveQrDir());
    }

    public String getMainDir() {
        String dir = props.getDir();
        return dir.endsWith("/") ? dir : dir + "/";
    }

    public String getPublicBaseUrl() {
        String url = props.getPublicBaseUrl();
        return url.endsWith("/") ? url : url + "/";
    }

    public String getLiveQrDir() {
        String dir = props.getLiveQrDir();
        return dir.endsWith("/") ? dir : dir + "/";
    }

    public String getLiveQrPublicPrefix() {
        String prefix = props.getLiveQrPublicPrefix();
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    public String getLivePlayPrefix() {
        return props.getLivePlayPrefix();
    }

    /**
     * 保存上传文件到主目录，返回公开 URL。
     */
    public String saveUpload(MultipartFile file) throws Exception {
        String original = file.getOriginalFilename();
        String suffix = original.substring(original.lastIndexOf("."));
        String filename = UUID.randomUUID().toString().replace("-", "") + suffix;

        File dest = new File(getMainDir() + filename);
        file.transferTo(dest);

        return getPublicBaseUrl() + filename;
    }

    /**
     * 保存上传文件到主目录，返回本地文件对象。
     */
    public File saveToFile(MultipartFile file) throws Exception {
        String original = file.getOriginalFilename();
        String suffix = original.substring(original.lastIndexOf("."));
        String filename = UUID.randomUUID().toString().replace("-", "") + suffix;

        File dest = new File(getMainDir() + filename);
        file.transferTo(dest);
        return dest;
    }

    /**
     * 获取 Live QR 海报文件路径。
     */
    public File getLiveQrFile(String streamKey) {
        return new File(getLiveQrDir() + streamKey + ".png");
    }

    /**
     * 获取 Live QR 原始二维码文件路径。
     */
    public File getLiveQrRawFile(String streamKey) {
        return new File(getLiveQrDir() + streamKey + "_raw.png");
    }

    private void ensureDir(String path) {
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建目录: " + path + "。请检查配置和文件权限。");
        }
    }
}
