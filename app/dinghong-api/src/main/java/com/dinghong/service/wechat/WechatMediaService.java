package com.dinghong.service.wechat;

import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 微信素材上传统一服务。
 * 替代 UploadController / LiveQrController 中重复的上传逻辑。
 */
@Service
public class WechatMediaService {

    /**
     * 上传图片到微信临时素材，返回 media_id。
     * @param accessToken 微信 access_token
     * @param file        本地图片文件
     * @return media_id，失败返回 ""
     */
    public String uploadImage(String accessToken, File file) {
        try {
            String uploadApi = "https://api.weixin.qq.com/cgi-bin/media/upload"
                    + "?access_token=" + accessToken + "&type=image";

            return doUpload(accessToken, file, uploadApi, "image/jpeg");
        } catch (Exception e) {
            System.out.println("[WECHAT_MEDIA_UPLOAD_ERROR] " + e.getMessage());
            return "";
        }
    }

    /**
     * 上传 PNG 图片到微信临时素材。
     */
    public String uploadImagePng(String accessToken, File file) {
        try {
            String uploadApi = "https://api.weixin.qq.com/cgi-bin/media/upload"
                    + "?access_token=" + accessToken + "&type=image";

            return doUpload(accessToken, file, uploadApi, "image/png");
        } catch (Exception e) {
            System.out.println("[WECHAT_MEDIA_UPLOAD_ERROR] " + e.getMessage());
            return "";
        }
    }

    private String doUpload(String accessToken, File file, String api, String contentType) throws Exception {
        String boundary = "----DINGHONG" + System.currentTimeMillis();

        HttpURLConnection conn = (HttpURLConnection) new URL(api).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream out = conn.getOutputStream();
             FileInputStream fis = new FileInputStream(file)) {

            out.write(("--" + boundary + "\r\n").getBytes());
            out.write(("Content-Disposition: form-data; name=\"media\"; filename=\""
                    + file.getName() + "\"\r\n").getBytes());
            out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes());

            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            out.write(("\r\n--" + boundary + "--\r\n").getBytes());
        }

        String json = read(conn.getInputStream());
        String mediaId = extractValue(json, "media_id");
        System.out.println("[WECHAT_MEDIA] upload done, media_id? " + (mediaId != null));
        return mediaId != null ? mediaId : "";
    }

    private String read(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private String extractValue(String json, String key) {
        if (json == null || key == null) return null;
        String mark = "\"" + key + "\":\"";
        int s = json.indexOf(mark);
        if (s == -1) return null;
        int start = s + mark.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
