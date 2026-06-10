package com.dinghong.service.ai;

import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class DeepSeekService {

    public String chat(String systemPrompt, String userPrompt) {
        try {
            String apiKey = System.getenv("DEEPSEEK_API_KEY");

            if (apiKey == null || apiKey.trim().isEmpty()) {
                return "DeepSeek API Key 未配置";
            }

            URL url = new URL("https://api.deepseek.com/chat/completions");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);

            String body = "{"
                    + "\"model\":\"deepseek-v4-pro\","
                    + "\"messages\":["
                    + "{\"role\":\"system\",\"content\":\"" + json(systemPrompt) + "\"},"
                    + "{\"role\":\"user\",\"content\":\"" + json(userPrompt) + "\"}"
                    + "],"
                    + "\"temperature\":0.8"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            InputStream in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();

            String json = read(in);

            return extractContent(json);

        } catch (Exception e) {
            return "DeepSeek调用失败：" + e.getMessage();
        }
    }

    private String read(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    private String json(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String extractContent(String json) {
        String key = "\"content\":\"";
        int s = json.indexOf(key);

        if (s == -1) {
            return json;
        }

        int start = s + key.length();
        StringBuilder sb = new StringBuilder();
        boolean escape = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escape) {
                if (c == 'n') sb.append('\n');
                else if (c == 'r') sb.append('\r');
                else if (c == 't') sb.append('\t');
                else sb.append(c);
                escape = false;
            } else {
                if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
        }

        return sb.toString();
    }
}
