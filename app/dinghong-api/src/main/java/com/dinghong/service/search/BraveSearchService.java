package com.dinghong.service.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Service
public class BraveSearchService {

    private static final String API_URL = "https://api.search.brave.com/res/v1/web/search";

    // Brave Search API Key
    private static final String BRAVE_API_KEY = "j9cqRqBbcMe8DkxKAYnooKwE";

    
    private boolean braveEnabled() {
        // 当前 Brave Key 返回 422 SUBSCRIPTION_TOKEN_INVALID，先禁用，避免污染百度资料包。
        return false;
    }

private final ObjectMapper objectMapper = new ObjectMapper();

    public String searchMatchInfo(String matchInfo, String category) {
        if (!braveEnabled()) {
            System.out.println("[BRAVE_SEARCH] disabled");
            return "";
        }

        if (BRAVE_API_KEY == null || BRAVE_API_KEY.trim().isEmpty()) {
            System.out.println("[BRAVE_SEARCH] skipped: api key empty");
            return "";
        }

        String cleanMatchInfo = matchInfo == null ? "" : matchInfo.trim();
        if (cleanMatchInfo.isEmpty()) {
            return "";
        }

        List<String> queries = buildQueries(cleanMatchInfo, category);
        StringBuilder all = new StringBuilder();

        for (String q : queries) {
            try {
                String one = callBrave(q);

                if (one != null && !one.trim().isEmpty()) {
                    all.append("\n\n[BRAVE_QUERY] ").append(q).append("\n");
                    all.append(one);
                }

                // 降低触发限流概率
                Thread.sleep(350);

            } catch (Exception e) {
                System.out.println("[BRAVE_SEARCH] error query=" + q + ", msg=" + e.getMessage());
            }
        }

        return all.toString();
    }

    private List<String> buildQueries(String matchInfo, String category) {
        List<String> list = new ArrayList<>();

        String noVs = matchInfo
                .replace(" VS ", " ")
                .replace(" vs ", " ")
                .replace("VS", " ")
                .replace("vs", " ")
                .replaceAll("\\s+", " ")
                .trim();

        boolean review = "REVIEW".equalsIgnoreCase(category);

        if (review) {
            list.add(matchInfo);
            list.add(noVs);
            list.add(noVs + " 比分");
            list.add(noVs + " 赛果");
            list.add(noVs + " 比赛结果");
            list.add(noVs + " 战报 全场");
            list.add(noVs + " 进球 技术统计 红黄牌");
        } else {
            list.add(matchInfo);
            list.add(noVs);
            list.add(noVs + " 赛前分析");
            list.add(noVs + " 阵容 伤停");
            list.add(noVs + " 近期状态");
            list.add(noVs + " 交锋");
        }

        return list;
    }

    private String callBrave(String query) throws Exception {
        String url = API_URL
                + "?q=" + URLEncoder.encode(query, "UTF-8")
                + "&country=CN"
                + "&search_lang=zh-hans"
                + "&ui_lang=zh-CN"
                + "&count=8"
                + "&freshness=pm"
                + "&extra_snippets=true"
                + "&safesearch=moderate";

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("X-Subscription-Token", BRAVE_API_KEY);

        int code = conn.getResponseCode();

        BufferedReader br = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                "UTF-8"
        ));

        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            body.append(line);
        }

        String raw = body.toString();

        System.out.println("[BRAVE_SEARCH] query=" + query);
        System.out.println("[BRAVE_SEARCH] http_code=" + code);
        System.out.println("[BRAVE_SEARCH] result_head=" + raw.substring(0, Math.min(raw.length(), 500)));

        if (code != 200) {
            return "[BRAVE_HTTP_" + code + "] " + raw.substring(0, Math.min(raw.length(), 500));
        }

        return parseBraveResult(raw);
    }

    private String parseBraveResult(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        JsonNode results = root.path("web").path("results");

        if (!results.isArray() || results.size() == 0) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        out.append("【Brave搜索结果】\n");

        int i = 0;
        for (JsonNode r : results) {
            if (i >= 8) break;
            i++;

            String title = text(r, "title");
            String url = text(r, "url");
            String description = text(r, "description");

            out.append(i).append(". 标题：").append(title).append("\n");
            out.append("   链接：").append(url).append("\n");
            out.append("   摘要：").append(description).append("\n");

            JsonNode extras = r.path("extra_snippets");
            if (extras.isArray()) {
                int n = 0;
                for (JsonNode ex : extras) {
                    if (n >= 3) break;

                    String t = ex.asText("");
                    if (!t.trim().isEmpty()) {
                        out.append("   补充摘要：").append(t).append("\n");
                        n++;
                    }
                }
            }

            out.append("\n");
        }

        return out.toString();
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("").replaceAll("\\s+", " ").trim();
    }
}
