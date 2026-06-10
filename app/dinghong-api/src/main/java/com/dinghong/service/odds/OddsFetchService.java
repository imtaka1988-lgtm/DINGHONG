package com.dinghong.service.odds;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OddsFetchService {

    @Value("${odds.api.enabled:true}")
    private boolean enabled;

    @Value("${odds.api.key:${ODDS_API_KEY:}}")
    private String apiKey;

    @Value("${odds.api.regions:eu}")
    private String regions;

    @Value("${odds.api.markets:h2h,spreads,totals}")
    private String markets;

    @Value("${odds.api.oddsFormat:decimal}")
    private String oddsFormat;

    @Value("${odds.api.footballSports:soccer_epl,soccer_uefa_champs_league,soccer_japan_j_league,soccer_spain_la_liga,soccer_italy_serie_a,soccer_germany_bundesliga,soccer_france_ligue_one,soccer_uefa_europa_league,soccer_usa_mls,soccer_fifa_world_cup,soccer_fifa_world_cup_winner,soccer_uefa_euro_qualification,soccer_uefa_nations_league,soccer_conmebol_copa_libertadores,soccer_england_efl_championship,soccer_netherlands_eredivisie,soccer_portugal_primeira_liga}")
    private String footballSports;

    @Value("${odds.api.basketballSports:basketball_nba,basketball_wnba,basketball_euroleague,basketball_ncaab,basketball_nbl,basketball_nba_summer_league}")
    private String basketballSports;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final Map<String, CacheItem> cache = new ConcurrentHashMap<>();

    public String fetchOdds(String matchInfo) {
        try {
            if (!enabled) {
                return noOdds("The Odds API 当前未启用。");
            }

            String key = effectiveApiKey();
            if (key.isEmpty()) {
                return noOdds("The Odds API Key 未配置。");
            }

            String cacheKey = normalize(matchInfo) + "|" + regions + "|" + markets;
            CacheItem cached = cache.get(cacheKey);
            if (cached != null && System.currentTimeMillis() - cached.time < 15 * 60 * 1000L) {
                return cached.value;
            }

            boolean basketball = isBasketball(matchInfo);
            List<String> sports = splitSports(basketball ? basketballSports : footballSports);

            MatchCandidate best = null;

            for (String sportKey : sports) {
                MatchCandidate candidate = fetchFromSport(sportKey, matchInfo);
                if (candidate != null && (best == null || candidate.score > best.score)) {
                    best = candidate;
                }

                if (best != null && best.score >= 95) {
                    break;
                }
            }

            String result;

            if (best == null || best.score < 70) {
                result = noOdds("未从 The Odds API 匹配到本场盘口。可能是该赛事不在当前套餐覆盖范围、比赛名称中英文不一致、盘口尚未开放，或当前只查询了配置中的赛事 sport key。");
            } else {
                result = formatOdds(best, basketball);
            }

            cache.put(cacheKey, new CacheItem(result));
            return result;

        } catch (Exception e) {
            System.out.println("[ODDS_API_ERROR] " + e.getMessage());
            return noOdds("盘口接口异常：" + e.getMessage());
        }
    }


    public String listSports() {
        try {
            String key = effectiveApiKey();
            if (key.isEmpty()) {
                return "The Odds API Key 未配置。";
            }

            String url = "https://api.the-odds-api.com/v4/sports?apiKey=" + enc(key);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String remaining = response.headers().firstValue("x-requests-remaining").orElse("-");
            String used = response.headers().firstValue("x-requests-used").orElse("-");

            if (response.statusCode() != 200) {
                return "The Odds API sports 请求失败，HTTP " + response.statusCode() + "\n" + response.body();
            }

            JsonNode arr = objectMapper.readTree(response.body());
            StringBuilder sb = new StringBuilder();
            sb.append("【The Odds API 当前可用运动列表】\n");
            sb.append("requests-used=").append(used).append("，requests-remaining=").append(remaining).append("\n\n");

            for (JsonNode n : arr) {
                String keyName = text(n, "key");
                String group = text(n, "group");
                String title = text(n, "title");
                String active = text(n, "active");

                if (keyName.contains("basketball")
                        || keyName.contains("soccer")
                        || group.toLowerCase(Locale.ROOT).contains("basketball")
                        || group.toLowerCase(Locale.ROOT).contains("soccer")) {
                    sb.append(keyName)
                            .append(" | ")
                            .append(group)
                            .append(" | ")
                            .append(title)
                            .append(" | active=")
                            .append(active)
                            .append("\n");
                }
            }

            return sb.toString();

        } catch (Exception e) {
            return "The Odds API sports 调试异常：" + e.getMessage();
        }
    }

    public String listCurrentEvents() {
        try {
            String key = effectiveApiKey();
            if (key.isEmpty()) {
                return "The Odds API Key 未配置。";
            }

            List<String> sports = new ArrayList<>();
            sports.addAll(splitSports(basketballSports));
            sports.addAll(splitSports(footballSports));

            StringBuilder sb = new StringBuilder();
            sb.append("【当前可抓取盘口赛事列表】\n");
            sb.append("说明：这里只显示 The Odds API 当前返回的 live/upcoming 赛事。这里没有的比赛，文章生成就无法从该源拿到真实盘口数字。\n\n");

            int total = 0;

            for (String sportKey : sports) {
                String url = "https://api.the-odds-api.com/v4/sports/" + enc(sportKey) + "/odds"
                        + "?apiKey=" + enc(key)
                        + "&regions=" + enc(regions)
                        + "&markets=" + enc(markets)
                        + "&oddsFormat=" + enc(oddsFormat)
                        + "&dateFormat=iso";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(25))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                String remaining = response.headers().firstValue("x-requests-remaining").orElse("-");
                String used = response.headers().firstValue("x-requests-used").orElse("-");

                sb.append("sport=").append(sportKey)
                        .append(" HTTP=").append(response.statusCode())
                        .append(" used=").append(used)
                        .append(" remaining=").append(remaining)
                        .append("\n");

                if (response.statusCode() != 200) {
                    sb.append("  请求失败：").append(shortText(response.body(), 180)).append("\n\n");
                    continue;
                }

                JsonNode arr = objectMapper.readTree(response.body());

                if (!arr.isArray() || arr.size() == 0) {
                    sb.append("  当前没有返回赛事。\n\n");
                    continue;
                }

                int count = 0;
                for (JsonNode event : arr) {
                    total++;
                    count++;
                    String home = text(event, "home_team");
                    String away = text(event, "away_team");
                    String time = text(event, "commence_time");
                    String title = text(event, "sport_title");

                    sb.append("  ")
                            .append(count)
                            .append(". ")
                            .append(home)
                            .append(" VS ")
                            .append(away)
                            .append(" | ")
                            .append(title)
                            .append(" | ")
                            .append(time)
                            .append("\n");

                    if (count >= 12) {
                        sb.append("  已省略更多赛事……\n");
                        break;
                    }
                }

                sb.append("\n");
            }

            sb.append("合计展示赛事数：").append(total).append("\n");
            return sb.toString();

        } catch (Exception e) {
            return "The Odds API events 调试异常：" + e.getMessage();
        }
    }

    private String effectiveApiKey() {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }
        String env = System.getenv("ODDS_API_KEY");
        return env == null ? "" : env.trim();
    }

    private String shortText(String s, int max) {
        if (s == null) return "";
        s = s.replace("\n", " ").replace("\r", " ").trim();
        if (s.length() > max) return s.substring(0, max) + "……";
        return s;
    }


    private MatchCandidate fetchFromSport(String sportKey, String matchInfo) {
        try {
            String url = "https://api.the-odds-api.com/v4/sports/" + enc(sportKey) + "/odds"
                    + "?apiKey=" + enc(effectiveApiKey())
                    + "&regions=" + enc(regions)
                    + "&markets=" + enc(markets)
                    + "&oddsFormat=" + enc(oddsFormat)
                    + "&dateFormat=iso";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String remaining = response.headers().firstValue("x-requests-remaining").orElse("-");
            String used = response.headers().firstValue("x-requests-used").orElse("-");
            System.out.println("[ODDS_API] sport=" + sportKey + " status=" + response.statusCode() + " used=" + used + " remaining=" + remaining);

            if (response.statusCode() != 200) {
                return null;
            }

            JsonNode arr = objectMapper.readTree(response.body());
            if (!arr.isArray()) return null;

            MatchCandidate best = null;

            for (JsonNode event : arr) {
                int score = scoreEvent(matchInfo, event);
                if (score <= 0) continue;

                if (best == null || score > best.score) {
                    best = new MatchCandidate(sportKey, event, score);
                }
            }

            return best;

        } catch (Exception e) {
            System.out.println("[ODDS_API_WARN] sport=" + sportKey + " " + e.getMessage());
            return null;
        }
    }

    private String formatOdds(MatchCandidate candidate, boolean basketball) {
        JsonNode event = candidate.event;

        String home = text(event, "home_team");
        String away = text(event, "away_team");
        String commence = text(event, "commence_time");
        String sportTitle = text(event, "sport_title");

        JsonNode bookmaker = pickBookmaker(event);
        String bookmakerTitle = bookmaker == null ? "未获取到明确公司" : text(bookmaker, "title");

        String h2h = "";
        String spreads = "";
        String totals = "";

        if (bookmaker != null && bookmaker.has("markets")) {
            for (JsonNode market : bookmaker.get("markets")) {
                String key = text(market, "key");
                if ("h2h".equals(key)) {
                    h2h = formatH2h(market);
                } else if ("spreads".equals(key)) {
                    spreads = formatSpreads(market);
                } else if ("totals".equals(key)) {
                    totals = formatTotals(market);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【真实盘口资料】\n");
        sb.append("数据源：The Odds API\n");
        sb.append("匹配度：").append(candidate.score).append("/100\n");
        sb.append("赛事：").append(sportTitle).append("\n");
        sb.append("匹配比赛：").append(home).append(" VS ").append(away).append("\n");
        if (!commence.isEmpty()) {
            sb.append("接口开赛时间：").append(commence).append("\n");
        }
        sb.append("盘口公司：").append(bookmakerTitle).append("\n");

        if (!h2h.isEmpty()) {
            sb.append(basketball ? "胜负欧赔：" : "胜平负欧赔：").append(h2h).append("\n");
        } else {
            sb.append(basketball ? "胜负欧赔：未获取到明确资料\n" : "胜平负欧赔：未获取到明确资料\n");
        }

        if (!spreads.isEmpty()) {
            sb.append(basketball ? "让分盘口：" : "让球盘口：").append(spreads).append("\n");
        } else {
            sb.append(basketball ? "让分盘口：未获取到明确资料\n" : "让球盘口：未获取到明确资料\n");
        }

        if (!totals.isEmpty()) {
            sb.append(basketball ? "大小分盘口：" : "大小球盘口：").append(totals).append("\n");
        } else {
            sb.append(basketball ? "大小分盘口：未获取到明确资料\n" : "大小球盘口：未获取到明确资料\n");
        }

        sb.append("写作约束：今日看法里的具体盘口数字，只允许使用上面已经明确出现的数字。没有明确出现的数字禁止编造。");
        return sb.toString();
    }

    private String noOdds(String reason) {
        return "【真实盘口资料】\n"
                + reason + "\n"
                + "写作约束：未获取到明确盘口数字时，禁止自行编造 -1、-4.5、168.5、2.5 等具体数字。"
                + "足球可写让胜思路、让平思路、让负思路、大球、小球；篮球可写主胜、客胜、主队让分思路、客队受让思路、大分、小分。";
    }

    private JsonNode pickBookmaker(JsonNode event) {
        if (!event.has("bookmakers") || !event.get("bookmakers").isArray() || event.get("bookmakers").size() == 0) {
            return null;
        }

        String[] preferred = {"bet365", "pinnacle", "unibet", "williamhill", "betfair", "bwin", "marathonbet"};

        for (String key : preferred) {
            for (JsonNode b : event.get("bookmakers")) {
                if (key.equalsIgnoreCase(text(b, "key"))) {
                    return b;
                }
            }
        }

        return event.get("bookmakers").get(0);
    }

    private String formatH2h(JsonNode market) {
        if (!market.has("outcomes")) return "";
        List<String> parts = new ArrayList<>();

        for (JsonNode o : market.get("outcomes")) {
            String name = text(o, "name");
            String price = price(o);
            if (!name.isEmpty() && !price.isEmpty()) {
                parts.add(name + " " + price);
            }
        }

        return String.join(" / ", parts);
    }

    private String formatSpreads(JsonNode market) {
        if (!market.has("outcomes")) return "";
        List<String> parts = new ArrayList<>();

        for (JsonNode o : market.get("outcomes")) {
            String name = text(o, "name");
            String point = point(o);
            String price = price(o);
            if (!name.isEmpty()) {
                StringBuilder one = new StringBuilder(name);
                if (!point.isEmpty()) one.append(" ").append(point);
                if (!price.isEmpty()) one.append(" @").append(price);
                parts.add(one.toString());
            }
        }

        return String.join(" / ", parts);
    }

    private String formatTotals(JsonNode market) {
        if (!market.has("outcomes")) return "";
        List<String> parts = new ArrayList<>();

        for (JsonNode o : market.get("outcomes")) {
            String name = text(o, "name");
            String point = point(o);
            String price = price(o);
            if (!name.isEmpty()) {
                String cn = "Over".equalsIgnoreCase(name) ? "大" : ("Under".equalsIgnoreCase(name) ? "小" : name);
                StringBuilder one = new StringBuilder(cn);
                if (!point.isEmpty()) one.append(point);
                if (!price.isEmpty()) one.append(" @").append(price);
                parts.add(one.toString());
            }
        }

        return String.join(" / ", parts);
    }

    private int scoreEvent(String matchInfo, JsonNode event) {
        String home = text(event, "home_team");
        String away = text(event, "away_team");

        String[] teams = extractTeams(matchInfo);
        String eventText = normalize(home + " " + away);

        if (teams.length >= 2) {
            boolean a = containsAnyAlias(eventText, teams[0]);
            boolean b = containsAnyAlias(eventText, teams[1]);

            if (a && b) return 100;
            if (a || b) return 45;
        }

        String m = normalize(matchInfo);
        String h = normalize(home);
        String a = normalize(away);

        if (h.length() >= 4 && a.length() >= 4 && m.contains(h) && m.contains(a)) {
            return 95;
        }

        int soft = 0;
        for (String token : tokens(matchInfo)) {
            if (token.length() >= 3 && eventText.contains(normalize(token))) {
                soft += 15;
            }
        }

        return Math.min(soft, 60);
    }

    private boolean containsAnyAlias(String normalizedEventText, String team) {
        for (String alias : aliases(team)) {
            String a = normalize(alias);
            if (a.length() >= 2 && normalizedEventText.contains(a)) {
                return true;
            }
        }
        return false;
    }

    private List<String> aliases(String team) {
        String n = normalize(team);
        List<String> out = new ArrayList<>();

        if (team != null && !team.trim().isEmpty()) {
            out.add(team.trim());
        }

        if (n.contains("中国男篮") || n.contains("中国队") || n.equals("中国")) {
            out.addAll(Arrays.asList("China", "China Men", "China Basketball", "Chinese Taipei"));
        }

        if (n.contains("fmp") || n.contains("拉德尼基")) {
            out.addAll(Arrays.asList("FMP", "FMP Beograd", "FMP Belgrade", "Radnicki", "Radni?ki"));
        }

        if (n.contains("纽约尼克斯") || n.contains("尼克斯")) {
            out.addAll(Arrays.asList("New York Knicks", "Knicks"));
        }

        if (n.contains("圣安东尼奥马刺") || n.contains("马刺")) {
            out.addAll(Arrays.asList("San Antonio Spurs", "Spurs"));
        }

        if (n.contains("湖人")) out.addAll(Arrays.asList("Los Angeles Lakers", "Lakers"));
        if (n.contains("勇士")) out.addAll(Arrays.asList("Golden State Warriors", "Warriors"));
        if (n.contains("凯尔特人")) out.addAll(Arrays.asList("Boston Celtics", "Celtics"));
        if (n.contains("独行侠") || n.contains("小牛")) out.addAll(Arrays.asList("Dallas Mavericks", "Mavericks"));
        if (n.contains("掘金")) out.addAll(Arrays.asList("Denver Nuggets", "Nuggets"));
        if (n.contains("快船")) out.addAll(Arrays.asList("Los Angeles Clippers", "Clippers"));
        if (n.contains("太阳")) out.addAll(Arrays.asList("Phoenix Suns", "Suns"));
        if (n.contains("雄鹿")) out.addAll(Arrays.asList("Milwaukee Bucks", "Bucks"));
        if (n.contains("热火")) out.addAll(Arrays.asList("Miami Heat", "Heat"));
        if (n.contains("雷霆")) out.addAll(Arrays.asList("Oklahoma City Thunder", "Thunder"));
        if (n.contains("森林狼")) out.addAll(Arrays.asList("Minnesota Timberwolves", "Timberwolves"));
        if (n.contains("火箭")) out.addAll(Arrays.asList("Houston Rockets", "Rockets"));
        if (n.contains("骑士")) out.addAll(Arrays.asList("Cleveland Cavaliers", "Cavaliers"));
        if (n.contains("公牛")) out.addAll(Arrays.asList("Chicago Bulls", "Bulls"));
        if (n.contains("猛龙")) out.addAll(Arrays.asList("Toronto Raptors", "Raptors"));

        if (n.contains("卡马塔马尔赞岐") || n.contains("赞岐") || n.contains("釜玉")) {
            out.addAll(Arrays.asList("Kamatamare Sanuki", "Sanuki"));
        }

        if (n.contains("长野") || n.contains("帕塞罗") || n.contains("琶扼搂")) {
            out.addAll(Arrays.asList("Nagano Parceiro", "Parceiro Nagano", "Nagano"));
        }


        // NBA
        if (n.contains("圣安东尼奥马刺") || n.contains("马刺")) out.addAll(Arrays.asList("San Antonio Spurs", "Spurs"));
        if (n.contains("纽约尼克斯") || n.contains("尼克斯")) out.addAll(Arrays.asList("New York Knicks", "Knicks"));

        // WNBA
        if (n.contains("印第安纳狂热") || n.contains("狂热")) out.addAll(Arrays.asList("Indiana Fever", "Fever"));
        if (n.contains("亚特兰大梦想") || n.contains("梦想")) out.addAll(Arrays.asList("Atlanta Dream", "Dream"));
        if (n.contains("明尼苏达山猫") || n.contains("山猫")) out.addAll(Arrays.asList("Minnesota Lynx", "Lynx"));
        if (n.contains("金州女武神") || n.contains("女武神")) out.addAll(Arrays.asList("Golden State Valkyries", "Valkyries"));
        if (n.contains("芝加哥天空") || n.contains("天空")) out.addAll(Arrays.asList("Chicago Sky", "Sky"));
        if (n.contains("康涅狄格太阳") || n.contains("阳光") || n.contains("太阳")) out.addAll(Arrays.asList("Connecticut Sun", "Sun"));
        if (n.contains("洛杉矶火花") || n.contains("火花")) out.addAll(Arrays.asList("Los Angeles Sparks", "Sparks"));
        if (n.contains("达拉斯飞翼") || n.contains("飞翼")) out.addAll(Arrays.asList("Dallas Wings", "Wings"));
        if (n.contains("波特兰火焰") || n.contains("火焰")) out.addAll(Arrays.asList("Portland Fire", "Fire"));
        if (n.contains("菲尼克斯水星") || n.contains("水星")) out.addAll(Arrays.asList("Phoenix Mercury", "Mercury"));

        // 日本J联赛
        if (n.contains("千叶市原") || n.contains("千叶")) out.addAll(Arrays.asList("JEF United Chiba", "JEF Chiba", "Chiba"));
        if (n.contains("福冈黄蜂") || n.contains("福冈")) out.addAll(Arrays.asList("Avispa Fukuoka", "Fukuoka"));
        if (n.contains("东京fc") || n.contains("fc东京")) out.addAll(Arrays.asList("FC Tokyo", "Tokyo"));
        if (n.contains("大阪樱花") || n.contains("大阪櫻花")) out.addAll(Arrays.asList("Cerezo Osaka", "Cerezo"));
        if (n.contains("鹿岛鹿角") || n.contains("鹿島鹿角")) out.addAll(Arrays.asList("Kashima Antlers", "Kashima"));
        if (n.contains("神户胜利船") || n.contains("神戶勝利船") || n.contains("神户")) out.addAll(Arrays.asList("Vissel Kobe", "Kobe"));
        if (n.contains("町田泽维亚") || n.contains("町田")) out.addAll(Arrays.asList("FC Machida Zelvia", "Machida Zelvia", "Machida"));
        if (n.contains("名古屋鲸八") || n.contains("名古屋鲸") || n.contains("名古屋")) out.addAll(Arrays.asList("Nagoya Grampus", "Nagoya"));
        if (n.contains("水户蜀葵") || n.contains("水户")) out.addAll(Arrays.asList("Mito HollyHock", "Mito"));
        if (n.contains("长崎成功丸") || n.contains("长崎")) out.addAll(Arrays.asList("V-Varen Nagasaki", "Nagasaki"));
        if (n.contains("浦和红钻") || n.contains("浦和")) out.addAll(Arrays.asList("Urawa Red Diamonds", "Urawa"));
        if (n.contains("冈山绿雉") || n.contains("冈山")) out.addAll(Arrays.asList("Fagiano Okayama", "Okayama"));
        if (n.contains("东京绿茵") || n.contains("东京绿茵")) out.addAll(Arrays.asList("Tokyo Verdy", "Verdy"));
        if (n.contains("大阪钢巴") || n.contains("大阪飛腳") || n.contains("钢巴")) out.addAll(Arrays.asList("Gamba Osaka", "Gamba"));
        if (n.contains("横滨水手") || n.contains("横滨f水手") || n.contains("横滨")) out.addAll(Arrays.asList("Yokohama F Marinos", "Yokohama Marinos", "Marinos"));
        if (n.contains("清水鼓动") || n.contains("清水心跳") || n.contains("清水")) out.addAll(Arrays.asList("Shimizu S Pulse", "Shimizu"));
        if (n.contains("柏太阳神") || n.contains("柏太陽神") || n.contains("柏")) out.addAll(Arrays.asList("Kashiwa Reysol", "Kashiwa"));
        if (n.contains("京都不死鸟") || n.contains("京都")) out.addAll(Arrays.asList("Kyoto Purple Sanga", "Kyoto Sanga", "Kyoto"));
        if (n.contains("川崎前锋") || n.contains("川崎")) out.addAll(Arrays.asList("Kawasaki Frontale", "Kawasaki"));
        if (n.contains("广岛三箭") || n.contains("廣島三箭") || n.contains("广岛")) out.addAll(Arrays.asList("Hiroshima Sanfrecce FC", "Sanfrecce Hiroshima", "Hiroshima"));

        return out;
    }

    private String[] extractTeams(String matchInfo) {
        if (matchInfo == null) return new String[0];

        String cleaned = matchInfo
                .replace("ＶＳ", "VS")
                .replace("ｖｓ", "VS")
                .replaceAll("(?i)\\s*vs\\.?\\s*", " VS ")
                .replaceAll("\\s*(对阵|迎战|大战)\\s*", " VS ")
                .replaceAll("(?<=[\\p{IsHan}A-Za-z0-9])\\s*[-－—–]\\s*(?=[\\p{IsHan}A-Za-z])", " VS ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] arr = cleaned.split("\\s+VS\\s+");
        if (arr.length >= 2) {
            return new String[]{cleanTeam(arr[0]), cleanTeam(arr[1])};
        }

        return new String[0];
    }

    private String cleanTeam(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)\\b(PREVIEW|REVIEW)\\b", "")
                .replaceAll("【[^】]*】", "")
                .replaceAll("\\d{1,2}[:：]\\d{2}", "")
                .replaceAll("\\d{1,2}[-/.月]\\d{1,2}日?", "")
                .replace("友谊赛", "")
                .replace("国际友谊", "")
                .replace("热身赛", "")
                .replace("足球", "")
                .replace("篮球", "")
                .trim();
    }

    private List<String> splitSports(String s) {
        List<String> list = new ArrayList<>();
        if (s == null) return list;

        for (String x : s.split(",")) {
            String v = x.trim();
            if (!v.isEmpty()) list.add(v);
        }

        return list;
    }

    private List<String> tokens(String s) {
        if (s == null) return Collections.emptyList();
        String cleaned = s.replaceAll("[^\\p{IsHan}A-Za-z0-9]+", " ");
        List<String> list = new ArrayList<>();
        for (String x : cleaned.split("\\s+")) {
            if (!x.trim().isEmpty()) list.add(x.trim());
        }
        return list;
    }

    private boolean isBasketball(String matchInfo) {
        if (matchInfo == null) return false;
        String t = matchInfo.toLowerCase(Locale.ROOT);
        return t.contains("nba")
                || t.contains("cba")
                || t.contains("wnba")
                || t.contains("basketball")
                || t.contains("篮球")
                || t.contains("男篮")
                || t.contains("女篮")
                || t.contains("湖人")
                || t.contains("勇士")
                || t.contains("凯尔特人")
                || t.contains("独行侠")
                || t.contains("尼克斯")
                || t.contains("马刺")
                || t.contains("掘金")
                || t.contains("快船")
                || t.contains("太阳")
                || t.contains("雄鹿")
                || t.contains("热火")
                || t.contains("森林狼")
                || t.contains("雷霆")
                || t.contains("火箭")
                || t.contains("骑士")
                || t.contains("公牛")
                || t.contains("猛龙")

                || t.contains("wnba")
                || t.contains("fever")
                || t.contains("dream")
                || t.contains("lynx")
                || t.contains("valkyries")
                || t.contains("sky")
                || t.contains("connecticut sun")
                || t.contains("sparks")
                || t.contains("wings")
                || t.contains("portland fire")
                || t.contains("mercury")
                || t.contains("indiana")
                || t.contains("atlanta")
                || t.contains("minnesota")
                || t.contains("golden state valkyries")
                || t.contains("chicago sky")
                || t.contains("los angeles sparks")
                || t.contains("dallas wings")
                || t.contains("phoenix mercury")
                || t.contains("印第安纳狂热")
                || t.contains("狂热")
                || t.contains("亚特兰大梦想")
                || t.contains("梦想")
                || t.contains("明尼苏达山猫")
                || t.contains("山猫")
                || t.contains("金州女武神")
                || t.contains("女武神")
                || t.contains("芝加哥天空")
                || t.contains("天空")
                || t.contains("康涅狄格太阳")
                || t.contains("阳光")
                || t.contains("洛杉矶火花")
                || t.contains("火花")
                || t.contains("达拉斯飞翼")
                || t.contains("飞翼")
                || t.contains("波特兰火焰")
                || t.contains("火焰")
                || t.contains("菲尼克斯水星")
                || t.contains("水星");
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace(".", "")
                .replace("'", "")
                .replace("’", "")
                .replace("：", "")
                .replace(":", "")
                .trim();
    }

    private String text(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) return "";
        return n.get(field).asText("");
    }

    private String price(JsonNode n) {
        if (n == null || !n.has("price") || n.get("price").isNull()) return "";
        return trimNumber(n.get("price").asText(""));
    }

    private String point(JsonNode n) {
        if (n == null || !n.has("point") || n.get("point").isNull()) return "";
        return trimNumber(n.get("point").asText(""));
    }

    private String trimNumber(String s) {
        if (s == null) return "";
        if (s.endsWith(".0")) return s.substring(0, s.length() - 2);
        return s;
    }

    private String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static class MatchCandidate {
        final String sportKey;
        final JsonNode event;
        final int score;

        MatchCandidate(String sportKey, JsonNode event, int score) {
            this.sportKey = sportKey;
            this.event = event;
            this.score = score;
        }
    }

    private static class CacheItem {
        final String value;
        final long time = System.currentTimeMillis();

        CacheItem(String value) {
            this.value = value;
        }
    }
}
