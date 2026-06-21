package com.dinghong.service.research;

import com.dinghong.service.ai.DeepSeekService;
import com.dinghong.service.search.BaiduSearchService;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class MatchResearchService {

    private final BaiduSearchService baiduSearchService;
    private final DeepSeekService deepSeekService;

    public MatchResearchService(BaiduSearchService baiduSearchService,
                                DeepSeekService deepSeekService) {
        this.baiduSearchService = baiduSearchService;
        this.deepSeekService = deepSeekService;
    }

    public String research(String matchInfo) {
        return research(matchInfo, "PREVIEW");
    }

    public String research(String matchInfo, String category) {

        if ("REVIEW".equalsIgnoreCase(category)) {
            ReviewResearchResult review = researchReview(matchInfo);
            if (review.isBlocked()) {
                return "REVIEW_BLOCKED：" + review.getBlockedReason();
            }
            return review.getMaterial();
        }

        String raw = baiduSearchService.searchMatchInfo(matchInfo, category);

        if (isSearchUnavailable(raw)) {
            return "PREVIEW_BLOCKED：" + searchUnavailableReason(raw) + "禁止生成赛前预测文章。";
        }

        boolean rawHasBothTeams = hasTeamEvidence(matchInfo, raw);
        boolean rawHasAnyTeam = hasAnyTeamEvidence(matchInfo, raw);

        if (!rawHasAnyTeam) {
            System.out.println("[PREVIEW_BLOCKED] 原始资料未命中任一球队。match=" + maskMatchInfo(matchInfo));
            return "PREVIEW_BLOCKED：未检索到该比赛任一球队的有效联网资料，禁止生成赛前预测文章。";
        }

        if (!rawHasBothTeams) {
            System.out.println("[PREVIEW_WARN] 原始资料未同时命中双方队名，继续整理但按资料有限处理。match=" + maskMatchInfo(matchInfo));
        }

        String prompt =
                "下面是百度搜索返回的赛事相关资料，请整理成适合AI写公众号赛前预测文章参考的中文资料包。\n\n" +
                "比赛：" + matchInfo + "\n\n" +
                "搜索资料：\n" +
                raw + "\n\n" +
                "整理要求：\n" +
                "1. 只提取搜索资料中明确出现的信息。\n" +
                "2. 如果只检索到一方球队资料，必须明确写资料有限，另一方写未获取到明确资料。\n" +
                "3. 不要编造任何伤停、首发、历史交锋、近期战绩、积分排名。\n" +
                "4. 删除网址、JSON字段、无关代码。\n" +
                "5. 中文输出。\n" +
                "6. 禁止Markdown符号。\n" +
                "7. 控制在1000字以内。\n\n" +
                "输出结构：\n" +
                "【联网赛事资料】\n" +
                "一、比赛时间与背景\n" +
                "二、伤停信息\n" +
                "三、近期状态与历史交锋\n" +
                "四、值得关注的比赛因素\n" +
                "五、写作限制";

        String result = deepSeekService.chat(
                "你是顶红体育资料编辑，只负责整理搜索资料，不做预测，不编造。",
                prompt
        );

        String cleaned = clean(result);

        if (cleaned.contains("PREVIEW_BLOCKED")) {
            return "PREVIEW_BLOCKED：未检索到该比赛的有效资料，禁止生成赛前预测文章。";
        }

        if (cleaned.trim().isEmpty()
                || cleaned.contains("DeepSeek API Key 未配置")
                || cleaned.contains("DeepSeek调用失败")) {
            System.out.println("[PREVIEW_BLOCKED] 赛前资料整理失败。match=" + maskMatchInfo(matchInfo));
            return "PREVIEW_BLOCKED：赛前联网资料整理失败，禁止生成赛前预测文章。";
        }

        return cleaned;
    }

    public ReviewResearchResult researchReview(String matchInfo) {

        if (hasManualResultEvidence(matchInfo)) {
            String material = buildManualReviewMaterial(matchInfo);
            System.out.println("[REVIEW_MANUAL_RESULT] 使用人工明确赛果生成复盘资料。match=" + maskMatchInfo(matchInfo));
            return ReviewResearchResult.passed(matchInfo, material, matchInfo);
        }

        String raw = baiduSearchService.searchMatchInfo(matchInfo, "REVIEW");

        if (isSearchUnavailable(raw)) {
            String reason = searchUnavailableReason(raw) + "禁止生成赛后复盘文章。";
            System.out.println("[REVIEW_BLOCKED] " + reason + " match=" + maskMatchInfo(matchInfo));
            return ReviewResearchResult.blocked(matchInfo, reason, raw);
        }

        boolean rawHasTeam = hasTeamEvidence(matchInfo, raw);
        boolean rawHasResult = hasResultEvidence(raw);

        if (!rawHasTeam) {
            System.out.println("[REVIEW_WARN] 原始资料未强命中双方队名，继续交给整理模型判断。match=" + maskMatchInfo(matchInfo));
        }

        if (!rawHasResult) {
            System.out.println("[REVIEW_WARN] 原始资料未强命中赛果关键词，继续交给整理模型判断。match=" + maskMatchInfo(matchInfo));
        }

        String prompt =
                "下面是百度搜索返回的赛后资料，请整理成适合AI写公众号赛后复盘文章参考的中文资料包。\n\n" +
                "比赛：" + matchInfo + "\n\n" +
                "搜索资料：\n" +
                raw + "\n\n" +
                "你必须严格判断搜索资料中是否存在明确赛果。\n\n" +
                "以下都算明确赛果证据：\n" +
                "1. 出现 0-3、3:0、3比0、1-1 这类比分。\n" +
                "2. 出现 不敌、战胜、击败、负于、战平、完场、全场、赛果、比赛结果、战报 等结果表达。\n" +
                "3. 出现双方队名之一或比赛标题，同时出现比分，也可视为赛果证据。\n\n" +
                "不要因为资料没有技术统计、没有红黄牌、没有采访就判断比赛未结束。\n" +
                "只有在资料完全没有最终比分、胜负结果、完场信息时，才输出：\n" +
                "REVIEW_BLOCKED：未检索到明确赛果，禁止生成复盘文章。\n\n" +
                "如果搜索资料中存在明确赛果，再继续整理。\n\n" +
                "整理要求：\n" +
                "1. 只提取搜索资料中明确出现的信息。\n" +
                "2. 必须优先提取最终比分、比赛结果、进球球员、红黄牌、技术统计、关键事件、赛后采访。\n" +
                "3. 不要编造任何比分、进球、红黄牌、控球率、射门数、赛后采访。\n" +
                "4. 资料没有明确给出的项目，必须写未获取到明确资料。\n" +
                "5. 删除网址、JSON字段、无关代码。\n" +
                "6. 中文输出。\n" +
                "7. 禁止Markdown符号。\n" +
                "8. 控制在1200字以内。\n\n" +
                "输出结构：\n" +
                "【赛后联网资料】\n" +
                "一、最终赛果与比分\n" +
                "二、进球与关键事件\n" +
                "三、红黄牌与争议点\n" +
                "四、技术统计与比赛走势\n" +
                "五、赛后声音\n" +
                "六、复盘写作限制";

        String result = deepSeekService.chat(
                "你是顶红体育资料编辑，只负责整理搜索资料，不做预测，不编造。",
                prompt
        );

        String cleaned = clean(result);

        if (cleaned.contains("REVIEW_BLOCKED")) {
            System.out.println("[REVIEW_BLOCKED] 资料整理模型判定阻断。match=" + maskMatchInfo(matchInfo));
            return ReviewResearchResult.blocked(matchInfo, extractBlockedReason(cleaned), raw);
        }

        if (cleaned.trim().isEmpty()) {
            System.out.println("[REVIEW_BLOCKED] 赛后资料整理为空。match=" + maskMatchInfo(matchInfo));
            return ReviewResearchResult.blocked(matchInfo, "赛后资料整理为空，禁止生成复盘文章。", raw);
        }

        boolean cleanedHasTeam = hasTeamEvidence(matchInfo, cleaned);
        boolean cleanedHasResult = hasResultEvidence(cleaned);

        if (!rawHasTeam && !cleanedHasTeam) {
            System.out.println("[REVIEW_BLOCKED] 原始资料和整理资料均未命中双方队名。match=" + maskMatchInfo(matchInfo));
            return ReviewResearchResult.blocked(matchInfo, "未检索到该比赛双方球队的明确赛后资料，禁止生成复盘文章。", raw);
        }

        if (!rawHasResult && !cleanedHasResult) {
            System.out.println("[REVIEW_BLOCKED] 原始资料和整理资料均未包含明确赛果。match=" + maskMatchInfo(matchInfo));
            return ReviewResearchResult.blocked(matchInfo, "未检索到该比赛的明确赛果，禁止生成复盘文章。", raw);
        }

        return ReviewResearchResult.passed(matchInfo, cleaned, raw);
    }


    private boolean hasTeamEvidence(String matchInfo, String text) {
        if (matchInfo == null || text == null) return false;

        String[] teams = extractTeams(matchInfo);
        String normalizedText = normalizeForMatch(text);

        if (teams.length < 2) {
            String normalizedMatch = normalizeForMatch(cleanTeamName(matchInfo));
            return normalizedMatch.length() >= 2 && normalizedText.contains(normalizedMatch);
        }

        boolean hasHome = hasTeamNameOrAlias(normalizedText, teams[0]);
        boolean hasAway = hasTeamNameOrAlias(normalizedText, teams[1]);

        return hasHome && hasAway;
    }

    private boolean hasAnyTeamEvidence(String matchInfo, String text) {
        if (matchInfo == null || text == null) return false;

        String[] teams = extractTeams(matchInfo);
        String normalizedText = normalizeForMatch(text);

        if (teams.length < 2) {
            String normalizedMatch = normalizeForMatch(cleanTeamName(matchInfo));
            return normalizedMatch.length() >= 2 && normalizedText.contains(normalizedMatch);
        }

        return hasTeamNameOrAlias(normalizedText, teams[0])
                || hasTeamNameOrAlias(normalizedText, teams[1]);
    }

    private boolean hasTeamNameOrAlias(String normalizedText, String teamName) {
        if (normalizedText == null || teamName == null) return false;

        String n = normalizeForMatch(cleanTeamName(teamName));
        if (n.length() >= 2 && normalizedText.contains(n)) {
            return true;
        }

        for (String alias : teamAliases(teamName)) {
            String a = normalizeForMatch(alias);
            if (a.length() >= 2 && normalizedText.contains(a)) {
                return true;
            }
        }

        return false;
    }

    private String[] teamAliases(String teamName) {
        String n = normalizeForMatch(cleanTeamName(teamName));

        if (n.contains("巴黎圣日耳曼") || n.contains("巴黎圣日尔曼") || n.equals("巴黎") || n.contains("psg") || n.contains("parissaintgermain")) {
            return new String[] {
                    "巴黎圣日耳曼", "巴黎圣日尔曼", "巴黎", "大巴黎",
                    "PSG", "Paris Saint-Germain", "Paris Saint Germain"
            };
        }

        if (n.contains("阿森纳") || n.contains("arsenal") || n.contains("枪手")) {
            return new String[] { "阿森纳", "Arsenal", "枪手" };
        }

        if (n.contains("德国") || n.contains("germany") || n.contains("deutschland")) {
            return new String[] { "德国", "德国队", "Germany", "Deutschland" };
        }

        if (n.contains("芬兰") || n.contains("finland")) {
            return new String[] { "芬兰", "芬兰队", "Finland" };
        }

        if (n.contains("巴西") || n.contains("brazil")) {
            return new String[] { "巴西", "巴西队", "Brazil", "Selecao" };
        }

        if (n.contains("巴拿马") || n.contains("panama")) {
            return new String[] { "巴拿马", "巴拿马队", "Panama" };
        }

        if (n.contains("拜仁") || n.contains("bayern")) {
            return new String[] { "拜仁", "拜仁慕尼黑", "Bayern", "Bayern Munich" };
        }

        if (n.contains("皇马") || n.contains("皇家马德里") || n.contains("realmadrid")) {
            return new String[] { "皇马", "皇家马德里", "Real Madrid" };
        }

        if (n.contains("巴萨") || n.contains("巴塞罗那") || n.contains("barcelona")) {
            return new String[] { "巴萨", "巴塞罗那", "Barcelona" };
        }

        return new String[0];
    }

    private String[] extractTeams(String matchInfo) {
        if (matchInfo == null) return new String[0];

        String cleaned = normalizeVsSeparator(matchInfo);
        String[] arr = cleaned.split("\\s+VS\\s+", 2);
        if (arr.length < 2) return new String[0];

        String home = cleanTeamName(arr[0]);
        String away = cleanTeamName(arr[1]);

        if (home.isEmpty() || away.isEmpty()) return new String[0];

        return new String[] { home, away };
    }

    private String normalizeVsSeparator(String text) {
        if (text == null) return "";

        return text
                .replace("ＶＳ", "VS")
                .replace("ｖｓ", "VS")
                .replace("Ｖｓ", "VS")
                .replace("ｖＳ", "VS")
                .replaceAll("(?i)(?<![A-Za-z])\\s*vs\\.?\\s*(?![A-Za-z])", " VS ")
                .replaceAll("(?i)\\s+v\\.?\\s+", " VS ")
                .replaceAll("\\s*(对阵|迎战|大战)\\s*", " VS ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanTeamName(String name) {
        if (name == null) return "";

        String cleaned = name.trim();

        cleaned = cleaned.replace("【足球赛事】", "")
                .replace("【篮球赛事】", "")
                .replace("足球赛事", "")
                .replace("篮球赛事", "")
                .replaceAll("北京时间.*$", "")
                .replaceAll("20[0-9]{2}[-/.年]\\s*\\d{1,2}[-/.月]\\s*\\d{1,2}日?.*$", "")
                .replaceAll("\\d{1,2}[-/.月]\\s*\\d{1,2}日?.*$", "")
                .replaceAll("\\d{1,2}[:：]\\d{2}.*$", "")
                .replaceAll("\\s*(——|--).*$", "")
                .replaceAll("\\s+[—–－-]\\s+.*$", "")
                .replaceAll("[（(].*$", "")
                .trim();

        cleaned = cleaned.replaceAll("^[^:：,，｜|]{1,30}[:：,，｜|]", "").trim();

        String[] prefixes = new String[] {
                "足球", "篮球", "国际友谊", "友谊赛", "热身赛",
                "中超", "中甲", "中乙", "中冠", "英超", "西甲", "德甲", "意甲", "法甲",
                "欧冠", "欧联", "欧协联", "欧洲杯", "世界杯", "亚洲杯", "世俱杯",
                "NBA", "CBA", "WNBA", "欧篮联", "篮联", "篮甲", "篮超", "土篮"
        };

        for (String prefix : prefixes) {
            if (cleaned.startsWith(prefix + " ")) {
                cleaned = cleaned.substring(prefix.length()).trim();
                break;
            }
            if (cleaned.startsWith(prefix) && cleaned.length() > prefix.length() + 1) {
                cleaned = cleaned.substring(prefix.length()).trim();
                break;
            }
        }

        return cleaned.trim();
    }

    private String normalizeForMatch(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("\\s+", "")
                .replace("：", ":")
                .replace("－", "-")
                .replace("—", "-")
                .replace("–", "-")
                .replace("，", ",")
                .replace("。", "")
                .replace("、", "")
                .replace("｜", "|")
                .replace("-", "")
                .trim();
    }


    private boolean isSearchUnavailable(String raw) {
        if (raw == null || raw.trim().isEmpty()) return true;

        String lower = raw.toLowerCase();

        if (raw.contains("Key未配置")
                || raw.contains("Unauthorized")
                || raw.contains("QUOTA_USER_DAILY_FREE")
                || raw.contains("Daily free quota")
                || lower.contains("quota")) {
            return true;
        }

        if (raw.contains("获取失败")) {
            String withoutFailure = raw.replaceAll("百度搜索资料获取失败：[^\n]*", "").trim();
            return withoutFailure.length() < 80;
        }

        return false;
    }

    private String searchUnavailableReason(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "未获取到联网资料。";
        }
        if (raw.contains("QUOTA_USER_DAILY_FREE") || raw.contains("Daily free quota") || raw.toLowerCase().contains("quota")) {
            return "百度联网搜索额度已用尽，暂时无法获取赛事资料。";
        }
        if (raw.contains("Key未配置")) {
            return "百度联网搜索Key未配置，暂时无法获取赛事资料。";
        }
        if (raw.contains("Unauthorized")) {
            return "百度联网搜索认证失败，暂时无法获取赛事资料。";
        }
        if (raw.contains("获取失败")) {
            return "百度联网搜索获取失败，暂时无法获取赛事资料。";
        }
        return "未获取到有效联网资料。";
    }

    private String extractBlockedReason(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "未检索到明确赛果，禁止生成复盘文章。";
        }
        return text.replace("REVIEW_BLOCKED：", "")
                   .replace("REVIEW_BLOCKED:", "")
                   .trim();
    }

    /**
     * 检查 matchInfo 是否包含明确的人工赛果标志。
     * 必须同时满足：①有比分表达、②有终局词、③无预测噪音。
     * 示例：
     *   "比分预测 2-1" → false（有预测噪音）
     *   "最终比分 2-1" → true（明确终局词）
     */
    /**
     * 仅用于单元测试的公开入口。
     */
    boolean hasManualResultEvidencePublic(String matchInfo) {
        return hasManualResultEvidence(matchInfo);
    }

    private boolean hasManualResultEvidence(String matchInfo) {
        if (matchInfo == null || matchInfo.trim().isEmpty()) return false;

        String text = matchInfo.replace("\\n", "")
                .replace("\\\"", "")
                .replace(" ", "")
                .replace("比", ":")
                .replace("：", ":");

        boolean hasScore = Pattern.compile("\\d+\\s*[:：比-]\\s*\\d+").matcher(matchInfo).find()
                || Pattern.compile("\\d+[:：-]\\d+").matcher(text).find();

        // 必须是明确终局词，不含泛化"比分"或"结束"
        boolean hasResultWord =
                text.contains("最终比分") ||
                text.contains("全场比分") ||
                text.contains("赛果") ||
                text.contains("比赛结果") ||
                text.contains("完场") ||
                text.contains("全场") ||
                text.contains("完赛") ||
                text.contains("已结束") ||
                text.contains("战胜") ||
                text.contains("击败") ||
                text.contains("不敌") ||
                text.contains("负于") ||
                text.contains("战平");

        // 排除预测类表达
        boolean hasPreviewNoise =
                text.contains("比分预测") ||
                text.contains("预测比分") ||
                text.contains("参考比分") ||
                text.contains("赛前预测");

        return hasScore && hasResultWord && !hasPreviewNoise;
    }

    private String buildManualReviewMaterial(String matchInfo) {
        String cleanedMatch = matchInfo == null ? "" : matchInfo.trim();
        return "【赛后联网资料】\n"
                + "一、最终赛果与比分\n"
                + "后台人工输入已提供明确赛果：" + cleanedMatch + "\n"
                + "二、进球与关键事件\n"
                + "未获取到明确资料。\n"
                + "三、红黄牌与争议点\n"
                + "未获取到明确资料。\n"
                + "四、技术统计与比赛走势\n"
                + "未获取到明确资料。\n"
                + "五、赛后声音\n"
                + "未获取到明确资料。\n"
                + "六、复盘写作限制\n"
                + "本场复盘允许基于后台人工输入的明确最终比分进行推荐结算；未提供的进球球员、红黄牌、技术统计和采访禁止编造。";
    }

    private boolean hasResultEvidence(String raw) {
        if (raw == null) return false;

        String text = raw.replace("\\n", "")
                         .replace("\\\"", "")
                         .replace(" ", "")
                         .replace("比", ":")
                         .replace("：", ":");

        boolean hasScore = Pattern.compile("\\d+\\s*[:：比-]\\s*\\d+").matcher(raw).find()
                || Pattern.compile("\\d+[:：-]\\d+").matcher(text).find();

        boolean hasResultWord =
                text.contains("最终比分") ||
                text.contains("全场比分") ||
                text.contains("赛果") ||
                text.contains("比赛结果") ||
                text.contains("完场") ||
                text.contains("完赛") ||
                text.contains("已结束") ||
                text.contains("击败") ||
                text.contains("战胜") ||
                text.contains("不敌") ||
                text.contains("负于") ||
                text.contains("告负") ||
                text.contains("大胜") ||
                text.contains("小胜") ||
                text.contains("战平") ||
                text.contains("逼平") ||
                text.contains("淘汰") ||
                text.contains("夺冠") ||
                text.contains("战报") ||
                text.contains("全场") ||
                text.contains("点球") ||
                text.contains("加时") ||
                text.contains("技术统计") ||
                text.contains("进球");

        boolean hasPreviewNoise =
                text.contains("比分预测") ||
                text.contains("预测比分") ||
                text.contains("比分参考") ||
                text.contains("赛前预测");

        boolean pass = hasScore && (hasResultWord || !hasPreviewNoise);

        System.out.println("[REVIEW_CHECK] hasScore=" + hasScore
                + ", hasResultWord=" + hasResultWord
                + ", hasPreviewNoise=" + hasPreviewNoise
                + ", pass=" + pass);

        return pass;
    }

    private String maskMatchInfo(String info) {
        if (info == null || info.length() <= 20) return info;
        return info.substring(0, 20) + "...";
    }

    private String clean(String text) {
        if (text == null) return "";
        return text.replace("#", "")
                   .replace("*", "")
                   .replace("```", "")
                   .replace("---", "")
                   .trim();
    }
}
