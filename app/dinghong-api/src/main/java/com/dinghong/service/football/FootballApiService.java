package com.dinghong.service.football;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Service
public class FootballApiService {

    public String buildMatchData(String matchInfo) {
        try {
            String[] teams = splitTeams(matchInfo);
            if (teams.length < 2) {
                return "未识别到双方球队，禁止编造具体数据。";
            }

            String home = normalizeTeam(teams[0]);
            String away = normalizeTeam(teams[1]);

            String homeId = searchTeamId(home);
            String awayId = searchTeamId(away);

            StringBuilder sb = new StringBuilder();
            sb.append("【真实足球数据资料】\n");
            sb.append("比赛：").append(matchInfo).append("\n\n");

            sb.append("主队搜索名：").append(home).append("\n");
            sb.append("客队搜索名：").append(away).append("\n\n");

            if (!homeId.isEmpty()) {
                sb.append("主队近期赛程资料：\n");
                sb.append(getLastFixtures(homeId)).append("\n\n");
            }

            if (!awayId.isEmpty()) {
                sb.append("客队近期赛程资料：\n");
                sb.append(getLastFixtures(awayId)).append("\n\n");
            }

            if (!homeId.isEmpty() && !awayId.isEmpty()) {
                sb.append("双方历史交锋资料：\n");
                sb.append(getHeadToHead(homeId, awayId)).append("\n\n");
            }

            sb.append("写作要求：只能引用以上真实资料，不允许编造伤停、近况、交锋、排名。");

            return sb.toString();

        } catch (Exception e) {
            return "足球数据接口获取失败，写作时不得编造具体伤停、近况、历史交锋和排名。";
        }
    }

    private String[] splitTeams(String matchInfo) {
        return matchInfo.replace("VS", "vs")
                .replace("Vs", "vs")
                .replace(" v ", " vs ")
                .split("vs");
    }

    private String normalizeTeam(String name) {
        name = name.trim();

        if (name.contains("阿森纳")) return "Arsenal";
        if (name.contains("巴黎")) return "Paris Saint Germain";
        if (name.contains("圣日耳曼")) return "Paris Saint Germain";
        if (name.contains("曼城")) return "Manchester City";
        if (name.contains("利物浦")) return "Liverpool";
        if (name.contains("皇马")) return "Real Madrid";
        if (name.contains("皇家马德里")) return "Real Madrid";
        if (name.contains("巴萨")) return "Barcelona";
        if (name.contains("巴塞罗那")) return "Barcelona";
        if (name.contains("拜仁")) return "Bayern Munich";
        if (name.contains("切尔西")) return "Chelsea";
        if (name.contains("曼联")) return "Manchester United";
        if (name.contains("热刺")) return "Tottenham";
        if (name.contains("尤文")) return "Juventus";
        if (name.contains("国米")) return "Inter";
        if (name.contains("国际米兰")) return "Inter";
        if (name.contains("AC米兰")) return "AC Milan";
        if (name.contains("马竞")) return "Atletico Madrid";

        return name;
    }

    private String searchTeamId(String teamName) throws Exception {
        String json = request("/teams?search=" + URLEncoder.encode(teamName, "UTF-8"));
        return extract(json, "\"id\":", ",");
    }

    private String getLastFixtures(String teamId) throws Exception {
        return limit(request("/fixtures?team=" + teamId + "&season=2025"), 3500);
    }

    private String getHeadToHead(String homeId, String awayId) throws Exception {
        return "免费版暂不调用历史交锋接口，避免权限限制。";
    }

    private String request(String path) throws Exception {
        String key = System.getenv("API_FOOTBALL_KEY");

        URL url = new URL("https://v3.football.api-sports.io" + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("x-apisports-key", key);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    private String extract(String text, String start, String end) {
        int s = text.indexOf(start);
        if (s < 0) return "";
        s += start.length();
        int e = text.indexOf(end, s);
        if (e < 0) return "";
        return text.substring(s, e).trim();
    }

    private String limit(String text, int max) {
        if (text == null) return "";
        if (text.length() > max) {
            return text.substring(0, max);
        }
        return text;
    }
}
