package com.dinghong.service.editor;

import com.dinghong.service.ai.DeepSeekService;
import com.dinghong.service.research.MatchResearchService;
import com.dinghong.service.research.ReviewResearchResult;
import com.dinghong.service.rule.RulePromptService;
import com.dinghong.service.odds.OddsFetchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Service
public class EditorService {

    @Autowired
    private OddsFetchService oddsFetchService;


    private final DataSource dataSource;
    private final DeepSeekService deepSeekService;
    private final MatchResearchService matchResearchService;
    private final RulePromptService rulePromptService;

    public EditorService(DataSource dataSource,
                         DeepSeekService deepSeekService,
                         MatchResearchService matchResearchService,
                         RulePromptService rulePromptService) {
        this.dataSource = dataSource;
        this.deepSeekService = deepSeekService;
        this.matchResearchService = matchResearchService;
        this.rulePromptService = rulePromptService;
    }

    public String writeArticle(String author, String category, String matchInfo) {
        return writeArticle(author, category, matchInfo, "");
    }

    public String writeArticle(String author, String category, String matchInfo, String relatedContent) {

        String promptCode = getPromptCode(author);
        String systemPrompt = getPrompt(promptCode);

        String categoryText = "REVIEW".equals(category) ? "昨日复盘" : "今日预测";
        String hardRule = rulePromptService.globalWritingRule() + "\n"
                + ("REVIEW".equals(category) ? rulePromptService.reviewArticleRule(matchInfo) : rulePromptService.previewArticleRule(matchInfo));
        systemPrompt = systemPrompt + "\n\n" + hardRule;

        String researchInfo;

        if ("REVIEW".equals(category)) {
            ReviewResearchResult reviewResearch = matchResearchService.researchReview(matchInfo);
            if (reviewResearch.isBlocked()) {
                String reason = reviewResearch.getBlockedReason();
                if (reason != null && (
                        reason.contains("额度已用尽")
                        || reason.contains("Key未配置")
                        || reason.contains("认证失败")
                        || reason.contains("搜索获取失败")
                )) {
                    throw new ReviewBlockedException(reason);
                }

                throw new ReviewBlockedException(
                        "系统未检索到该比赛的明确赛果，本场暂不生成赛后复盘。请检查比赛名称、比分资料或稍后重试。"
                );
            }
            researchInfo = reviewResearch.getMaterial();
        } else {
            researchInfo = matchResearchService.research(matchInfo, category);
            if (researchInfo != null && researchInfo.contains("PREVIEW_BLOCKED")) {
                throw new ArticleBlockedException(
                        "preview_blocked",
                        "未检索到该比赛的有效联网资料，禁止生成赛前预测文章。请检查比赛名称是否准确。"
                );
            }
        }

        String predictionExtractInfo = "";

        if ("REVIEW".equals(category)) {
            predictionExtractInfo = extractPredictionSummary(relatedContent);
            if (predictionExtractInfo.contains("PREDICTION_EXTRACT_BLOCKED")) {
                throw new ArticleBlockedException(
                        "review_blocked",
                        "关联预测文章未提取到方向、大小球或比分参考，禁止生成赛后复盘。请检查赛前预测文章格式。"
                );
            }
        }

        String userPrompt =
                "你现在要以第一人称写一篇顶红体育公众号文章。\n\n" +
                "作者：" + authorName(author) + "\n" +
                "文章类型：" + categoryText + "\n" +
                "比赛：" + matchInfo + "\n\n" +
                "以下是系统联网获取的最新比赛资料，请优先参考。\n" +
                ("REVIEW".equals(category)
                        ? "这是赛后复盘，必须严格基于联网资料里的明确赛果、比分、进球、红黄牌、技术统计和关键事件来写。没有明确资料的内容禁止编造；资料未提供的项目必须写未获取到明确资料。\n\n"
                        + rulePromptService.reviewRule(matchInfo)
                        : "如果资料里没有明确提到的内容，禁止编造具体伤停、近5场战绩、历史交锋、积分排名。\n\n") +
                researchInfo + "\n\n" +
                ("REVIEW".equals(category) && relatedContent != null && !relatedContent.trim().isEmpty()
                        ? "这是昨日预测原文，请你基于它来复盘。\n"
                        + "注意：必须先按顶红体育欧洲盘结算规则判断昨日推荐是否命中。\n"
                        + "如果昨日推荐在90分钟加伤停补时内命中，必须明确写命中，不能因为加时赛、点球大战或最终冠军归属而说预测错了。\n\n"
                        + "【赛前预测结构化提取】\n"
                        + predictionExtractInfo + "\n\n"
                        + "复盘时必须优先按照上面的结构化提取逐项验证：方向、大小球、比分参考。\n"
                        + "禁止绕开结构化提取，自行猜测昨日观点。\n\n"
                        + "昨日预测原文：\n" + relatedContent + "\n\n"
                        : "") +
                "【最高优先级统一规则】\n" +
                hardRule + "\n" +
                "写作要求：\n" +
                "1. 必须像真人体育专栏，不要像AI报告。\n" +
                "2. 不要写“作为AI”。\n" +
                "3. 不要四个人同时发言，只能由当前作者主写。\n" +
                "4. 不要自己写PS，PS由系统自动追加。\n" +
                "5. 禁止使用稳单、必中、包红、跟单、梭哈、重注、稳赚、内部消息等词。\n" +
                "6. 文章中只能出现这四个编辑名字：老周、阿凯、老唐、小北。\n" +
                "7. 禁止虚构其他人物，例如隔壁老李、老王、朋友、群友等。\n" +
                "8. 文章长度按精简短文执行：主体分析控制在180到300字左右；全文包含方向、大小球、比分参考后控制在250到400字左右。小众赛事和资料有限赛事必须更短，禁止AI长文和资料堆砌。\n" +
                "9. 如果联网资料里出现明确开赛时间，正文最前面必须单独写一行：比赛时间：北京时间 MM-DD HH:mm。\n" +
                "10. 如果资料里没有明确开赛时间，不要编造时间。\n" +
                ("REVIEW".equals(category)
                        ? "11. 赛后复盘必须先用1到2句写昨日推荐结算，再用一小段写比赛核心过程，全文保持250到400字左右。\n"
                        + "12. 结算部分必须严谨；比赛复盘部分可以庆祝获胜队伍、分析球员表现、战术变化和球迷情绪。\n"
                        + "13. 加时赛和点球大战只能作为比赛故事补充，不能影响常规推荐结算。\n"
                        + "14. 如果昨日推荐命中，必须明确承认命中；不能嘴硬说错。\n"
                        : "11. 赛前预测结尾必须使用规定的玩法推荐格式，足球必须写主任方向、主任看法、大小球、推荐比分；篮球必须写主任方向、让分看法、大小分、分差参考。禁止写成方向/进球数或总分/比分参考这种旧格式。\n") +
                "\n文章结构：\n" +
                ("REVIEW".equals(category)
                        ? "标题\n"
                        + "昨日推荐结算\n"
                        + "核心复盘\n"
                        + "复盘结论\n"
                        : "标题\n正文\n今日看法\n") +
                "\n";

        String generated = cleanArticle(deepSeekService.chat(systemPrompt, userPrompt));
        generated = rulePromptService.sanitizeForbiddenWords(generated);
        if (!"REVIEW".equals(category)) {
            generated = enforcePreviewPlayFormat(generated, matchInfo);
            generated = enforceBasketballSpreadGap(generated, matchInfo);

            String oddsOverrideInfo = "";
            try {
                oddsOverrideInfo = oddsFetchService.fetchOdds(matchInfo);
            } catch (Exception e) {
                System.out.println("[ODDS_OVERRIDE_WARN] " + e.getMessage());
            }

            generated = enforceOddsPreviewBlock(generated, oddsOverrideInfo, matchInfo);
            generated = enforceOddsMatchTime(generated, oddsOverrideInfo);
        }
        String matchTimeLine = buildMatchTimeLine(researchInfo);

        if (!matchTimeLine.isEmpty() && !containsMatchTime(generated)) {
            generated = matchTimeLine + "\n\n" + generated;
        }

        return generated;
    }




    private String enforceBasketballSpreadGap(String text, String matchInfo) {
        if (text == null) return "";
        if (!isBasketballMatch(matchInfo)) return text;
        if (!text.contains("让分看法：") || !text.contains("分差参考：")) return text;

        String spreadLine = "";
        for (String line : text.split("\\r?\\n")) {
            if (line.startsWith("让分看法：")) {
                spreadLine = line;
                break;
            }
        }

        if (spreadLine.isEmpty()) return text;

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("([+-]\\d+(?:\\.5)?)")
                .matcher(spreadLine);

        if (!m.find()) {
            return text;
        }

        double point;
        try {
            point = Double.parseDouble(m.group(1));
        } catch (Exception e) {
            return text;
        }

        double abs = Math.abs(point);
        boolean favorite = point < 0;

        String newGap;

        if (favorite) {
            if (abs >= 8.5) {
                newGap = "分差参考：主队8到12分";
            } else if (abs >= 7.5) {
                newGap = "分差参考：主队8到12分";
            } else if (abs >= 5.5) {
                newGap = "分差参考：主队6到10分";
            } else if (abs >= 2.5) {
                newGap = "分差参考：主队3到7分";
            } else {
                newGap = "分差参考：主队1到5分";
            }
        } else {
            if (abs >= 8.5) {
                newGap = "分差参考：受让方有机会守住，两队分差预计在个位数";
            } else if (abs >= 5.5) {
                newGap = "分差参考：受让方有机会守住，分差预计不大";
            } else {
                newGap = "分差参考：双方分差预计在个位数";
            }
        }

        return text.replaceFirst("(?m)^分差参考：.*$", java.util.regex.Matcher.quoteReplacement(newGap));
    }


    private String enforcePreviewPlayFormat(String text, String matchInfo) {
        if (text == null) return "";

        boolean basketball = isBasketballMatch(matchInfo);
        String out = text;

        // 先修复历史错误
        out = out.replace("主任主任方向：", "主任方向：")
                 .replace("主任主任方向:", "主任方向：");

        // 只替换“行首的旧标签”，避免把主任方向替换成主任主任方向
        out = out.replaceAll("(?m)^方向[:：]", "主任方向：")
                 .replaceAll("(?m)^进球数/总分[:：]", basketball ? "大小分：" : "大小球：")
                 .replaceAll("(?m)^总分[:：]", "大小分：")
                 .replaceAll("(?m)^进球数[:：]", "大小球：")
                 .replaceAll("(?m)^比分参考[:：]", basketball ? "分差参考：" : "推荐比分：");

        if (basketball) {
            out = out.replace("中国男篮不败", "中国男篮胜")
                     .replace("主队不败", "主胜")
                     .replace("客队不败", "客胜")
                     .replace("主任方向：主队方向", "主任方向：主胜")
                     .replace("主任方向：客队方向", "主任方向：客胜")
                     .replace("让分看法：主队方向", "让分看法：主队让分思路")
                     .replace("让分看法：客队方向", "让分看法：客队受让思路")
                     .replace("大小分：大分方向", "大小分：大分")
                     .replace("大小分：小分方向", "大小分：小分")
                     .replace("分差参考：双方分差不大", "分差参考：分差预计在个位数");
        } else {
            out = out.replace("主任方向：主队方向", "主任方向：胜")
                     .replace("主任方向：客队方向", "主任方向：负")
                     .replace("大小球：大球方向", "大小球：大球")
                     .replace("大小球：小球方向", "大小球：小球")
                     .replace("大小球：进球数偏多", "大小球：大球")
                     .replace("大小球：进球数偏少", "大小球：小球");
        }

        out = out.replace("主任主任方向：", "主任方向：")
                 .replace("主任主任方向:", "主任方向：");

        return out;
    }


    private String enforceOddsMatchTime(String text, String oddsInfo) {
        if (text == null) return "";
        if (oddsInfo == null || !oddsInfo.contains("接口开赛时间：")) {
            return text;
        }

        String iso = extractAfterPrefix(oddsInfo, "接口开赛时间：");
        if (isBlank(iso)) return text;

        String bj;
        try {
            java.time.Instant instant = java.time.Instant.parse(iso.trim());
            java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.of("Asia/Shanghai"));
            bj = zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            return text;
        }

        String line = "比赛时间：北京时间 " + bj;

        if (text.contains("比赛时间：")) {
            return text.replaceFirst("(?m)^比赛时间：.*$", java.util.regex.Matcher.quoteReplacement(line));
        }

        return line + "\n\n" + text;
    }

    private String enforceOddsPreviewBlock(String text, String oddsInfo, String matchInfo) {
        if (text == null) return "";
        if (oddsInfo == null || !oddsInfo.contains("【真实盘口资料】") || !oddsInfo.contains("匹配度：")) {
            return text;
        }

        String block = buildOddsTodayView(oddsInfo, matchInfo);
        if (isBlank(block)) {
            return text;
        }

        int idx = text.indexOf("今日看法：");
        if (idx < 0) {
            return text.trim() + "\n\n" + block;
        }

        int psIdx = text.indexOf("\nPS", idx);
        if (psIdx < 0) {
            psIdx = text.indexOf("\n\nPS", idx);
        }

        if (psIdx >= 0) {
            return text.substring(0, idx).trim() + "\n\n" + block + "\n\n" + text.substring(psIdx).trim();
        }

        return text.substring(0, idx).trim() + "\n\n" + block;
    }

    private String buildOddsTodayView(String oddsInfo, String matchInfo) {
        boolean basketball = isBasketballMatch(matchInfo);

        String h2h = firstNonBlank(
                extractAfterPrefix(oddsInfo, "胜负欧赔："),
                extractAfterPrefix(oddsInfo, "胜平负欧赔：")
        );

        String spread = firstNonBlank(
                extractAfterPrefix(oddsInfo, "让分盘口："),
                extractAfterPrefix(oddsInfo, "让球盘口：")
        );

        String total = firstNonBlank(
                extractAfterPrefix(oddsInfo, "大小分盘口："),
                extractAfterPrefix(oddsInfo, "大小球盘口：")
        );

        if (basketball) {
            String direction = chooseH2hDirection(h2h, true, matchInfo);
            String spreadPick = chooseSpreadPick(spread, true);
            String totalPick = chooseTotalPick(total);
            String gap = "主胜".equals(direction) ? "主队1到5分" : ("客胜".equals(direction) ? "客队5分以内" : "分差预计在个位数");

            if (isBlank(direction)) direction = "主胜";
            if (isBlank(spreadPick)) spreadPick = "主队让分思路 1手";
            if (isBlank(totalPick)) totalPick = "小分 1手";

            return "今日看法：\n"
                    + "主任方向：" + direction + "\n"
                    + "让分看法：" + spreadPick + "\n"
                    + "大小分：" + totalPick + "\n"
                    + "分差参考：" + gap;
        }

        String direction = chooseH2hDirection(h2h, false, matchInfo);
        String spreadPick = chooseSpreadPick(spread, false);
        String totalPick = chooseTotalPick(total);
        String score = buildFootballScore(direction, totalPick);

        if (isBlank(direction)) direction = "平";
        if (isBlank(spreadPick)) spreadPick = "让平思路 1手";
        if (isBlank(totalPick)) totalPick = "小球 1手";

        return "今日看法：\n"
                + "主任方向：" + direction + "\n"
                + "主任看法：" + spreadPick + "\n"
                + "大小球：" + totalPick + "\n"
                + "推荐比分：" + score;
    }

    private String chooseH2hDirection(String h2h, boolean basketball, String matchInfo) {
        if (isBlank(h2h) || h2h.contains("未获取")) return "";

        OddsPick best = pickLowestPrice(h2h);
        if (best == null || isBlank(best.name)) return "";

        String name = best.name;
        String cn = toChineseTeam(name);
        String[] teams = splitMatchTeams(matchInfo);
        String home = teams.length > 0 ? teams[0] : "";
        String away = teams.length > 1 ? teams[1] : "";

        if (name.equalsIgnoreCase("Draw") || name.contains("平")) {
            return basketball ? "" : "平";
        }

        if (!isBlank(home) && (cn.contains(home) || home.contains(cn))) {
            return basketball ? "主胜" : "胜";
        }

        if (!isBlank(away) && (cn.contains(away) || away.contains(cn))) {
            return basketball ? "客胜" : "负";
        }

        if (h2h.indexOf(best.raw) > h2h.length() / 2) {
            return basketball ? "客胜" : "负";
        }

        return basketball ? "主胜" : "胜";
    }

    private String chooseSpreadPick(String spread, boolean basketball) {
        if (isBlank(spread) || spread.contains("未获取")) return "";

        OddsPick best = pickLowestPrice(spread);
        if (best == null) return "";

        String team = toChineseTeam(best.name);
        String point = normalizePoint(best.point);

        if (isBlank(team)) {
            team = best.name;
        }

        if (basketball) {
            if (isBlank(point)) {
                return team + "让分思路 1手";
            }
            return team + point + " 1手";
        }

        if (isBlank(point)) {
            return team + "让平思路 1手";
        }

        return team + point + " 1手";
    }

    private String chooseTotalPick(String total) {
        if (isBlank(total) || total.contains("未获取")) return "";

        OddsPick best = pickLowestPrice(total);
        if (best == null) return "";

        String raw = best.name;
        String cn;

        if (raw.equalsIgnoreCase("Over") || raw.contains("大")) {
            cn = "大";
        } else if (raw.equalsIgnoreCase("Under") || raw.contains("小")) {
            cn = "小";
        } else {
            cn = raw;
        }

        String point = normalizePoint(best.point);

        if (isBlank(point)) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(大|小)\\s*(\\d+(?:\\.5)?)").matcher(best.raw);
            if (m.find()) {
                return m.group(1) + m.group(2) + " 1手";
            }
            return cn + "分 1手";
        }

        return cn + point + " 1手";
    }

    private OddsPick pickLowestPrice(String line) {
        if (isBlank(line)) return null;

        String[] parts = line.split("\\s*/\\s*");
        OddsPick best = null;

        for (String part : parts) {
            String raw = part.trim();
            if (raw.isEmpty()) continue;

            java.util.regex.Matcher priceM = java.util.regex.Pattern.compile("@?(\\d+(?:\\.\\d+)?)\\s*$").matcher(raw);
            double price = 99.0;
            if (priceM.find()) {
                try {
                    price = Double.parseDouble(priceM.group(1));
                } catch (Exception ignored) {}
            }

            String beforePrice = raw.replaceAll("@?\\d+(?:\\.\\d+)?\\s*$", "").trim();

            String point = "";
            java.util.regex.Matcher pointM = java.util.regex.Pattern.compile("([+-]?\\d+(?:\\.5)?)\\s*$").matcher(beforePrice);
            if (pointM.find()) {
                point = pointM.group(1);
                beforePrice = beforePrice.substring(0, pointM.start()).trim();
            }

            String name = beforePrice.trim();

            OddsPick pick = new OddsPick();
            pick.raw = raw;
            pick.name = name;
            pick.point = point;
            pick.price = price;

            if (best == null || pick.price < best.price) {
                best = pick;
            }
        }

        return best;
    }

    private String buildFootballScore(String direction, String totalPick) {
        boolean small = totalPick != null && (totalPick.contains("小") || totalPick.contains("小球"));
        boolean big = totalPick != null && (totalPick.contains("大") || totalPick.contains("大球"));

        if ("负".equals(direction)) {
            return small ? "0-1 1-1" : "1-2 0-2";
        }

        if ("胜".equals(direction)) {
            return small ? "1-0 1-1" : "2-1 2-0";
        }

        if (big) {
            return "1-1 2-2";
        }

        return "0-0 1-1";
    }

    private String normalizePoint(String point) {
        if (point == null) return "";
        point = point.trim();
        if (point.endsWith(".0")) point = point.substring(0, point.length() - 2);
        return point;
    }

    private String toChineseTeam(String name) {
        if (name == null) return "";
        String n = name.toLowerCase();

        if (n.contains("avispa fukuoka")) return "福冈黄蜂";
        if (n.contains("jef united chiba") || n.contains("jef chiba")) return "千叶市原";
        if (n.contains("fc tokyo")) return "FC东京";
        if (n.contains("cerezo osaka")) return "大阪樱花";
        if (n.contains("kashima antlers")) return "鹿岛鹿角";
        if (n.contains("vissel kobe")) return "神户胜利船";
        if (n.contains("machida")) return "町田泽维亚";
        if (n.contains("nagoya grampus")) return "名古屋鲸八";
        if (n.contains("mito hollyhock")) return "水户蜀葵";
        if (n.contains("nagasaki")) return "长崎成功丸";
        if (n.contains("urawa")) return "浦和红钻";
        if (n.contains("okayama")) return "冈山绿雉";
        if (n.contains("tokyo verdy")) return "东京绿茵";
        if (n.contains("gamba osaka")) return "大阪钢巴";
        if (n.contains("yokohama")) return "横滨水手";
        if (n.contains("shimizu")) return "清水鼓动";
        if (n.contains("kashiwa")) return "柏太阳神";
        if (n.contains("kyoto")) return "京都不死鸟";
        if (n.contains("kawasaki")) return "川崎前锋";
        if (n.contains("hiroshima") || n.contains("sanfrecce")) return "广岛三箭";

        if (n.contains("indiana fever")) return "印第安纳狂热";
        if (n.contains("atlanta dream")) return "亚特兰大梦想";
        if (n.contains("san antonio spurs")) return "圣安东尼奥马刺";
        if (n.contains("new york knicks")) return "纽约尼克斯";

        return name;
    }

    private String[] splitMatchTeams(String matchInfo) {
        if (matchInfo == null) return new String[0];

        String cleaned = matchInfo
                .replaceAll("(?i)\\s*vs\\.?\\s*", " VS ")
                .replaceAll("\\s*(对阵|迎战|大战)\\s*", " VS ")
                .replaceAll("北京时间.*$", "")
                .replaceAll("\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}\\s*\\d{1,2}:\\d{2}", "")
                .replaceAll("\\d{1,2}[-/.月]\\d{1,2}日?\\s*\\d{1,2}:\\d{2}", "")
                .replaceAll("^(NBA|WNBA|日本J联赛|J联赛|日职联|日职|足球|篮球)\\s*", "")
                .trim();

        String[] arr = cleaned.split("\\s+VS\\s+");
        if (arr.length >= 2) {
            return new String[] { arr[0].trim(), arr[1].trim() };
        }

        return new String[0];
    }

    private String extractAfterPrefix(String text, String prefix) {
        if (text == null || prefix == null) return "";

        for (String line : text.split("\\r?\\n")) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }

        return "";
    }

    private static class OddsPick {
        String raw;
        String name;
        String point;
        double price;
    }


    private boolean isBasketballMatch(String matchInfo) {
        if (matchInfo == null) return false;
        String t = matchInfo.toLowerCase();
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
                || t.contains("掘金")
                || t.contains("快船")
                || t.contains("太阳")
                || t.contains("雄鹿")
                || t.contains("热火")
                || t.contains("尼克斯")
                || t.contains("森林狼")
                || t.contains("雷霆")
                || t.contains("76人")
                || t.contains("国王")
                || t.contains("灰熊")
                || t.contains("火箭")
                || t.contains("公牛")
                || t.contains("骑士")
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

    private String buildMatchTimeLine(String researchInfo) {
        String time = extractMatchTimeFromText(researchInfo);
        if (time.isEmpty()) {
            return "";
        }
        return "比赛时间：北京时间 " + time;
    }

    private boolean containsMatchTime(String text) {
        if (text == null) return false;
        String s = text.replace("：", ":");
        if (s.contains("北京时间")) return true;
        return java.util.regex.Pattern
                .compile("(20[0-9]{2}[-/.年](0?[1-9]|1[0-2])[-/.月](0?[1-9]|[12][0-9]|3[01])日?\\s*(20|21|22|23|[01]?[0-9])[:：][0-5][0-9])|((0?[1-9]|1[0-2])月(0?[1-9]|[12][0-9]|3[01])日?\\s*(20|21|22|23|[01]?[0-9])[:：][0-5][0-9])")
                .matcher(s)
                .find();
    }

    private String extractMatchTimeFromText(String text) {
        if (text == null || text.trim().isEmpty()) return "";

        String s = text.replace("\r", " ").replace("\n", " ").replace("T", " ");

        java.util.regex.Pattern[] patterns = new java.util.regex.Pattern[] {
                java.util.regex.Pattern.compile("(20[0-9]{2})[-/.年](0?[1-9]|1[0-2])[-/.月](0?[1-9]|[12][0-9]|3[01])日?\\s*(?:周[一二三四五六日天])?\\s*(20|21|22|23|[01]?[0-9])[:：]([0-5][0-9])"),
                java.util.regex.Pattern.compile("(0?[1-9]|1[0-2])月(0?[1-9]|[12][0-9]|3[01])日?\\s*(?:周[一二三四五六日天])?\\s*(20|21|22|23|[01]?[0-9])[:：]([0-5][0-9])"),
                java.util.regex.Pattern.compile("(0?[1-9]|1[0-2])[-/.](0?[1-9]|[12][0-9]|3[01])\\s+(20|21|22|23|[01]?[0-9])[:：]([0-5][0-9])")
        };

        java.util.regex.Matcher m = patterns[0].matcher(s);
        if (m.find()) {
            return twoDigit(m.group(2)) + "-" + twoDigit(m.group(3)) + " " + twoDigit(m.group(4)) + ":" + m.group(5);
        }

        for (int i = 1; i < patterns.length; i++) {
            m = patterns[i].matcher(s);
            if (m.find()) {
                return twoDigit(m.group(1)) + "-" + twoDigit(m.group(2)) + " " + twoDigit(m.group(3)) + ":" + m.group(4);
            }
        }

        return "";
    }

    private String twoDigit(String n) {
        try {
            int v = Integer.parseInt(n);
            return v < 10 ? "0" + v : String.valueOf(v);
        } catch (Exception e) {
            return n;
        }
    }

    private String extractPredictionSummary(String relatedContent) {
        if (relatedContent == null || relatedContent.trim().isEmpty()) {
            return "PREDICTION_EXTRACT_BLOCKED";
        }

        String text = relatedContent
                .replace("\r", "\n")
                .replace("　", " ")
                .trim();

        String direction = firstNonBlank(extractAfterLabel(text, "主任方向"), extractAfterLabel(text, "主任看法"), extractAfterLabel(text, "方向"));
        String totalGoals = firstNonBlank(extractAfterLabel(text, "大小球"), extractAfterLabel(text, "大小分"), extractAfterLabel(text, "让分看法"));
        String scoreReference = firstNonBlank(extractAfterLabel(text, "推荐比分"), extractAfterLabel(text, "比分参考"), extractAfterLabel(text, "分差参考"));

        if (isBlank(direction)) {
            direction = fallbackDirection(text);
        }

        if (isBlank(totalGoals)) {
            totalGoals = fallbackTotalGoals(text);
        }

        if (isBlank(scoreReference)) {
            scoreReference = fallbackScoreReference(text);
        }

        if (isBlank(direction) && isBlank(totalGoals) && isBlank(scoreReference)) {
            return "PREDICTION_EXTRACT_BLOCKED";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("方向：").append(isBlank(direction) ? "未提取到明确方向" : direction).append("\n");
        sb.append("大小球：").append(isBlank(totalGoals) ? "未提取到明确大小球" : totalGoals).append("\n");
        sb.append("比分参考：").append(isBlank(scoreReference) ? "未提取到明确比分参考" : scoreReference).append("\n");
        sb.append("结算要求：复盘必须逐项判断以上内容是否命中，不得自行改写昨日观点。");
        return sb.toString();
    }


    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (!isBlank(v)) return v;
        }
        return "";
    }

    private String extractAfterLabel(String text, String label) {
        if (text == null) return "";

        String[] lines = text.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = cleanLine(lines[i]);
            if (line.startsWith(label + "：") || line.startsWith(label + ":")) {
                String value = line.substring(line.indexOf(line.contains("：") ? "：" : ":") + 1).trim();
                if (!value.isEmpty()) {
                    return value;
                }

                StringBuilder sb = new StringBuilder();
                for (int j = i + 1; j < lines.length && j <= i + 3; j++) {
                    String next = cleanLine(lines[j]);
                    if (next.isEmpty()) continue;
                    if (isKnownNextLabel(next)) break;
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(next);
                }
                return sb.toString().trim();
            }
        }

        return "";
    }

    private boolean isKnownNextLabel(String line) {
        if (line == null) return false;
        return line.startsWith("主任方向：")
                || line.startsWith("主任方向:")
                || line.startsWith("主任看法：")
                || line.startsWith("主任看法:")
                || line.startsWith("方向：")
                || line.startsWith("方向:")
                || line.startsWith("让分看法：")
                || line.startsWith("让分看法:")
                || line.startsWith("大小球：")
                || line.startsWith("大小球:")
                || line.startsWith("大小分：")
                || line.startsWith("大小分:")
                || line.startsWith("推荐比分：")
                || line.startsWith("推荐比分:")
                || line.startsWith("分差参考：")
                || line.startsWith("分差参考:")
                || line.startsWith("比分参考：")
                || line.startsWith("比分参考:")
                || line.startsWith("PS")
                || line.startsWith("标题")
                || line.startsWith("今日看法")
                || line.startsWith("复盘结论");
    }

    private String fallbackDirection(String text) {
        String compact = text.replace("\n", " ");

        String[] keys = new String[] {
                "常规时间不败",
                "不败",
                "主胜",
                "客胜",
                "平局",
                "让胜",
                "让平",
                "让负"
        };

        for (String key : keys) {
            int idx = compact.indexOf(key);
            if (idx >= 0) {
                int start = Math.max(0, idx - 25);
                int end = Math.min(compact.length(), idx + 45);
                return compact.substring(start, end).trim();
            }
        }

        return "";
    }

    private String fallbackTotalGoals(String text) {
        String compact = text.replace("\n", " ");

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(大球|小球|大分|小分|大|小|大于|小于|超过|低于|不超过|不低于)\\s*\\d+(\\.5)?\\s*(球|分)?")
                .matcher(compact);

        if (m.find()) {
            return m.group();
        }

        return "";
    }

    private String fallbackScoreReference(String text) {
        String compact = text.replace("\n", " ");

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\d+\\s*[比:-]\\s*\\d+(\\s*[,，、/]\\s*\\d+\\s*[比:-]\\s*\\d+)?")
                .matcher(compact);

        if (m.find()) {
            return m.group();
        }

        return "";
    }

    private String cleanLine(String line) {
        if (line == null) return "";
        return line
                .replace("#", "")
                .replace("*", "")
                .replace("　", " ")
                .trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private String footballSettlementRule() {
        return "【顶红体育足球欧洲盘结算硬规则】\n"
                + "1. 除非昨日预测明确写了晋级、夺冠、捧杯、冠军归属，否则所有常规足球推荐一律按90分钟加伤停补时结算。\n"
                + "2. 90分钟包括上下半场和伤停补时，不包括加时赛、点球大战、淘汰赛最终晋级结果，也不包括赛后官方改判。\n"
                + "3. 胜平负、独赢、双重机会、不败、让球、大小球、总进球、比分参考、波胆，全部属于常规推荐，默认只看90分钟加伤停补时。\n"
                + "4. 只有晋级、夺冠、捧杯、冠军归属这类特殊预测，才允许计算加时赛和点球大战。\n"
                + "5. 如果昨日预测是某队不败，90分钟打平，则不败命中。即使该队后来加时或点球输掉冠军，也不影响常规结算。\n"
                + "6. 如果昨日预测是小于2.5球，只统计90分钟加伤停补时总进球数，不计算加时和点球大战。\n"
                + "7. 如果昨日预测比分参考是1比1，而90分钟比分是1比1，则比分参考命中。即使点球大战改变冠军归属，也不能说比分错了。\n"
                + "8. 复盘必须先判断昨日推荐项是否命中，再评论最终谁夺冠。\n"
                + "9. 禁止把点球大战胜负误判为90分钟胜负。\n"
                + "10. 推荐结算要严谨，但比赛复盘不能只写红黑，也要写球队表现、球员发挥、战术变化、关键转折和球迷情绪。\n\n";
    }

    private String cleanArticle(String text) {
        if (text == null) return "";

        int psIndex = text.indexOf("\nPS");
        if (psIndex >= 0) {
            text = text.substring(0, psIndex);
        }

        text = text.replace("###", "")
                   .replace("##", "")
                   .replace("#", "")
                   .replace("***", "")
                   .replace("**", "")
                   .replace("*", "")
                   .replace("```", "")
                   .replace("---", "")
                   .replace("稳如狗", "需要谨慎看待")
                   .replace("稳赢", "更倾向")
                   .replace("稳胆", "个人倾向")
                   .replace("必中", "值得关注")
                   .replace("包红", "值得观察")
                   .replace("立帖为证", "")
                   .replace("跟单", "参考")
                   .replace("上车", "关注")
                   .replace("收米", "")
                   .replaceAll("[（(]\\d{1,3}字[）)]", "")
                   .trim();

        return text;
    }

    private String getPromptCode(String author) {
        if ("laozhou".equals(author)) return "chief_editor";
        if ("akai".equals(author)) return "basketball_editor";
        if ("laotang".equals(author)) return "football_editor";
        if ("xiaobei".equals(author)) return "analyst_editor";
        return "chief_editor";
    }

    private String authorName(String author) {
        if ("laozhou".equals(author)) return "老周";
        if ("akai".equals(author)) return "阿凯";
        if ("laotang".equals(author)) return "老唐";
        if ("xiaobei".equals(author)) return "小北";
        return "老周";
    }

    private String getPrompt(String promptCode) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT prompt_content FROM ai_prompt WHERE prompt_code=? AND status='ENABLED' LIMIT 1";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, promptCode);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("prompt_content");
            }

        } catch (Exception e) {
            return "你是顶红体育编辑，请输出有个人风格的赛事文章。";
        }

        return "你是顶红体育编辑，请输出有个人风格的赛事文章。";
    }
}
