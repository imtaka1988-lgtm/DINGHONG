package com.dinghong.service.search;

import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class BaiduSearchService {

    public String searchMatchInfo(String matchInfo) {
        return searchMatchInfo(matchInfo, "PREVIEW");
    }

    public String searchMatchInfo(String matchInfo, String category) {
        String base = cleanQuery(matchInfo);

        if ("REVIEW".equalsIgnoreCase(category)) {
            StringBuilder all = new StringBuilder();

            all.append(callBaidu(base + " 比分 赛果", "month")).append("\n\n");
            all.append(callBaidu(base + " 战报 全场 比赛结果", "month")).append("\n\n");
            all.append(callBaidu(base + " 进球 技术统计 红黄牌", "month")).append("\n\n");

            return clean(all.toString());
        }

        /*
         * 赛前修复：
         * 原来只搜“最新伤停/预计首发/赛前新闻/近期状态”一条窄关键词，
         * 小众赛事、青年队、低级别联赛很容易没有伤停或首发页面，导致百度明明可查，系统却误判无资料。
         * 这里改成“赛程/比赛时间/前瞻/最新消息”优先，再补一条阵容状态，提升可查比赛的召回率。
         */
        StringBuilder all = new StringBuilder();
        all.append(callBaidu(base + " 比赛时间 赛程 赛前 前瞻 最新消息", "month")).append("\n\n");
        all.append(callBaidu(base + " 近期状态 阵容 伤停 预测", "month")).append("\n\n");

        return clean(all.toString());
    }

    private String cleanQuery(String matchInfo) {
        if (matchInfo == null) return "";

        return matchInfo
                .replace("【足球赛事】", "")
                .replace("【篮球赛事】", "")
                .replace("足球赛事", "")
                .replace("篮球赛事", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String callBaidu(String query, String recency) {
        try {
            String key = System.getenv("BAIDU_SEARCH_KEY");

            if (key == null || key.trim().isEmpty()) {
                System.out.println("[BAIDU_SEARCH] Key未配置");
                return "百度搜索Key未配置。";
            }

            System.out.println("[BAIDU_SEARCH] query=" + query);

            String body =
                    "{"
                    + "\"messages\":[{\"role\":\"user\",\"content\":\"" + json(query) + "\"}],"
                    + "\"search_source\":\"baidu_search_v2\","
                    + "\"resource_type_filter\":[{\"type\":\"web\",\"top_k\":10}],"
                    + "\"search_recency_filter\":\"" + recency + "\""
                    + "}";

            URL url = new URL("https://qianfan.baidubce.com/v2/ai_search/web_search");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Authorization", "Bearer " + key);
            conn.setRequestProperty("X-Appbuilder-Authorization", "Bearer " + key);
            conn.setRequestProperty("Content-Type", "application/json");

            try(OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String result = read(is);

            System.out.println("[BAIDU_SEARCH] http_code=" + code);
            System.out.println("[BAIDU_SEARCH] result_head=" + shortText(result, 1200));

            return clean(result);

        } catch(Exception e) {
            System.out.println("[BAIDU_SEARCH] error=" + e.getMessage());
            return "百度搜索资料获取失败：" + e.getMessage();
        }
    }

    private String clean(String text) {
        if (text == null) return "";

        if (text.length() > 16000) {
            text = text.substring(0, 16000);
        }

        return text.replace("#", "")
                   .replace("*", "")
                   .replace("```", "")
                   .replace("---", "")
                   .trim();
    }

    private String shortText(String text, int max) {
        if (text == null) return "";
        text = text.replace("\n", " ").replace("\r", " ");
        return text.length() > max ? text.substring(0, max) : text;
    }

    private String json(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String read(InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;

        while((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }
}
