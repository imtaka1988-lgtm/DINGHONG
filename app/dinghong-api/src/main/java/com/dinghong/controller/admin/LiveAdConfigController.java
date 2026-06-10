package com.dinghong.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/live")
public class LiveAdConfigController {

    private static final String LIVE_DIR = "/data/dinghong/admin/live";
    private static final File AD_CONFIG_FILE = new File(LIVE_DIR, "ad_config.json");
    private static final File BANNER_FILE = new File(LIVE_DIR, "banner.txt");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/ad-config")
    public Map<String, String> getAdConfig() {
        Map<String, String> result = defaultConfig();

        try {
            if (AD_CONFIG_FILE.exists()) {
                Map<?, ?> saved = objectMapper.readValue(AD_CONFIG_FILE, Map.class);
                result.put("top", clean(saved.get("top")));
                result.put("ad1", clean(saved.get("ad1")));
                result.put("ad2", clean(saved.get("ad2")));
                result.put("bottom", clean(saved.get("bottom")));
            }

            if (BANNER_FILE.exists()) {
                result.put("banner", clean(Files.readString(BANNER_FILE.toPath(), StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    @PutMapping("/ad-config")
    public Map<String, Object> saveAdConfig(@RequestBody Map<String, String> body) throws Exception {
        ensureLiveDir();

        Map<String, String> config = defaultConfig();
        config.put("top", normalizeUrl(body.get("top")));
        config.put("ad1", normalizeUrl(body.get("ad1")));
        config.put("ad2", normalizeUrl(body.get("ad2")));
        config.put("bottom", normalizeUrl(body.get("bottom")));

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(AD_CONFIG_FILE, config);

        String banner = normalizeUrl(body.get("banner"));
        Files.writeString(BANNER_FILE.toPath(), banner, StandardCharsets.UTF_8);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("message", "广告配置已保存");
        result.put("config", config);
        result.put("banner", banner);
        return result;
    }

    private Map<String, String> defaultConfig() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("top", "");
        map.put("ad1", "");
        map.put("ad2", "https://webcdn.pics/20260602/b05d7e82071f44afb927aba6737cc652.jpg");
        map.put("bottom", "https://webcdn.pics/20260602/b05d7e82071f44afb927aba6737cc652.jpg");
        map.put("banner", "");
        return map;
    }

    private void ensureLiveDir() throws Exception {
        File dir = new File(LIVE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建直播配置目录：" + LIVE_DIR);
        }
    }

    private String normalizeUrl(String value) {
        String url = clean(value);
        if (url.isEmpty()) return "";

        if (url.length() > 1000) {
            throw new IllegalArgumentException("图片地址过长");
        }

        String lower = url.toLowerCase();
        if (!lower.startsWith("https://") && !lower.startsWith("http://")) {
            throw new IllegalArgumentException("图片地址必须以 http:// 或 https:// 开头");
        }

        return url;
    }

    private String clean(Object value) {
        if (value == null) return "";
        return value.toString().trim();
    }
}
