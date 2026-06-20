package com.dinghong.controller.editor;

import com.dinghong.service.editor.EditorService;
import com.dinghong.service.editor.EditorPsService;
import com.dinghong.service.editor.WechatMetaService;
import com.dinghong.service.editor.ReviewBlockedException;
import com.dinghong.service.editor.ArticleBlockedException;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

@RestController
@RequestMapping("/editor")
public class ArticleController {

    private final EditorService editorService;
    private final EditorPsService editorPsService;
    private final DataSource dataSource;
    private final WechatMetaService wechatMetaService;

    public ArticleController(EditorService editorService,
                             EditorPsService editorPsService,
                             DataSource dataSource,
                             WechatMetaService wechatMetaService) {
        this.editorService = editorService;
        this.editorPsService = editorPsService;
        this.dataSource = dataSource;
        this.wechatMetaService = wechatMetaService;
    }

    @PostMapping("/generate-review/{id}")
    public String generateReview(@PathVariable Long id,
                                 @RequestParam(required = false) String resultInfo,
                                 @RequestParam(required = false) String finalScore) {

        String matchInfo;
        String author;
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT title, author_editor FROM article_task WHERE id=? AND article_category='PREVIEW' LIMIT 1"
            );
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                matchInfo = rs.getString("title");
                author = rs.getString("author_editor");
            } else {
                return "error:未找到对应的赛前预测文章";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }

        if (matchInfo == null || matchInfo.trim().isEmpty()) {
            return "review_blocked:预测文章标题为空，无法提取比赛信息";
        }

        String manualResult = buildManualResultInfo(resultInfo, finalScore);
        if (!manualResult.isEmpty()) {
            matchInfo = matchInfo + "\n" + manualResult;
        }

        return generate(matchInfo, author, "REVIEW", id);
    }

    @PostMapping("/generate")
    public String generate(@RequestParam String matchInfo,
                           @RequestParam(defaultValue = "akai") String author,
                           @RequestParam(defaultValue = "PREVIEW") String category,
                           @RequestParam(required = false) Long relatedArticleId) {

        try {
            if ("REVIEW".equalsIgnoreCase(category) && relatedArticleId == null) {
                return "review_blocked:赛后复盘必须关联上一篇赛前预测文章。请先选择对应的赛前预测文章，再生成复盘。";
            }

            String relatedContent = "";

            if (relatedArticleId != null) {
                relatedContent = getRelatedArticleContent(relatedArticleId);
            }

            if ("REVIEW".equalsIgnoreCase(category) && (relatedContent == null || relatedContent.trim().isEmpty())) {
                return "review_blocked:未读取到关联的赛前预测原文，禁止生成赛后复盘。";
            }

            Long matchId = resolveMatchId(matchInfo, relatedArticleId);

            String sportType = resolveSportType(matchInfo, relatedArticleId, relatedContent, author);

            /*
             * 注意：
             * sportType 只写入 article_task.sport_type，供封面、插图、公众号样式判断。
             * 不再把【足球赛事】/【篮球赛事】拼进 matchInfo，
             * 否则会污染百度检索关键词，导致“已结束说未结束 / 百度可查却找不到资料”。
             */
            String content = editorService.writeArticle(author, category, matchInfo, relatedContent);
            String ps = editorPsService.randomPs(author, content);
            String finalContent = content + ps;

            String wechatTitle = wechatMetaService.generateTitle(finalContent);
            String wechatSummary = wechatMetaService.generateSummary(finalContent);
            String coverText = wechatMetaService.generateCoverText(matchInfo, author, category);
            String coverHeadline = wechatMetaService.generateCoverHeadline(finalContent);

            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement psSql = conn.prepareStatement(
                        "INSERT INTO article_task(" +
                                "title, article_type, status, author_editor, article_category, related_article_id, match_id, sport_type, final_content, wechat_title, wechat_summary, cover_text, cover_headline" +
                                ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)"
                );

                psSql.setString(1, matchInfo);
                psSql.setString(2, category);
                psSql.setString(3, "APPROVED");
                psSql.setString(4, author);
                psSql.setString(5, category);

                if (relatedArticleId == null) {
                    psSql.setNull(6, java.sql.Types.BIGINT);
                } else {
                    psSql.setLong(6, relatedArticleId);
                }

                if (matchId == null) {
                    psSql.setNull(7, java.sql.Types.BIGINT);
                } else {
                    psSql.setLong(7, matchId);
                }

                psSql.setString(8, sportType);
                psSql.setString(9, finalContent);
                psSql.setString(10, wechatTitle);
                psSql.setString(11, wechatSummary);
                psSql.setString(12, coverText);
                psSql.setString(13, coverHeadline);
                psSql.executeUpdate();
            }

            return "success";

        } catch (ArticleBlockedException e) {
            return e.getCode() + ":" + e.getReason();
        } catch (ReviewBlockedException e) {
            return "review_blocked:" + e.getReason();
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }


    private Long resolveMatchId(String matchInfo, Long relatedArticleId) {
        if (relatedArticleId != null) {
            Long relatedMatchId = getRelatedMatchId(relatedArticleId);
            if (relatedMatchId != null) {
                return relatedMatchId;
            }
        }

        return findMatchIdByMatchInfo(matchInfo);
    }

    private Long getRelatedMatchId(Long articleId) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT match_id FROM article_task WHERE id=? LIMIT 1"
            );
            ps.setLong(1, articleId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                long id = rs.getLong("match_id");
                if (!rs.wasNull()) {
                    return id;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private Long findMatchIdByMatchInfo(String matchInfo) {
        String text = matchInfo == null ? "" : matchInfo.trim().toLowerCase();
        if (text.isEmpty()) return null;

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id,home_team,away_team,league_name FROM match_live ORDER BY id DESC LIMIT 500"
            );
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String home = safe(rs.getString("home_team")).toLowerCase();
                String away = safe(rs.getString("away_team")).toLowerCase();
                String league = safe(rs.getString("league_name")).toLowerCase();

                if (!home.isEmpty() && !away.isEmpty() && text.contains(home) && text.contains(away)) {
                    return rs.getLong("id");
                }

                if (!league.isEmpty() && text.contains(league) && ((!home.isEmpty() && text.contains(home)) || (!away.isEmpty() && text.contains(away)))) {
                    return rs.getLong("id");
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }


    private String resolveSportType(String matchInfo, Long relatedArticleId, String relatedContent, String author) {
        // 复盘优先继承赛前文章 sport_type
        if (relatedArticleId != null) {
            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT sport_type,title,final_content FROM article_task WHERE id=? LIMIT 1"
                );
                ps.setLong(1, relatedArticleId);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String inherited = normalizeSportType(rs.getString("sport_type"));
                    if (!inherited.isEmpty()) return inherited;

                    String detected = detectSportType(safe(rs.getString("title")) + " " + safe(rs.getString("final_content")));
                    if (!detected.isEmpty()) return detected;
                }
            } catch (Exception ignored) {}
        }

        // 优先从 match_live 表查询 sport_type（数据库记录最优先）
        String dbSport = findSportTypeFromMatchLive(matchInfo);
        if (dbSport != null) return dbSport;

        String detected = detectSportType(safe(matchInfo) + " " + safe(relatedContent));
        if (!detected.isEmpty()) return detected;

        // 默认足球，避免足球复盘被篮球弱词误伤
        return "football";
    }

    private String findSportTypeFromMatchLive(String matchInfo) {
        if (matchInfo == null || matchInfo.trim().isEmpty()) return null;
        String text = normalizeForSportMatch(matchInfo);
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT sport_type, home_team, away_team, league_name "
                    + "FROM match_live ORDER BY id DESC LIMIT 500"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String home = normalizeForSportMatch(safe(rs.getString("home_team")));
                String away = normalizeForSportMatch(safe(rs.getString("away_team")));
                String league = normalizeForSportMatch(safe(rs.getString("league_name")));
                // 必须命中双方队名或联赛+一方队名
                boolean matchTeams = !home.isEmpty() && !away.isEmpty()
                        && text.contains(home) && text.contains(away);
                boolean matchLeague = !league.isEmpty() && text.contains(league)
                        && ((!home.isEmpty() && text.contains(home))
                            || (!away.isEmpty() && text.contains(away)));
                if (matchTeams || matchLeague) {
                    String sport = rs.getString("sport_type");
                    if (sport != null && !sport.trim().isEmpty()) {
                        String n = normalizeSportType(sport);
                        if (!n.isEmpty()) return n;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String normalizeForSportMatch(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replace("－", "-")
                .replace("—", "-")
                .replace("–", "-");
    }

    /** 已知篮球俱乐部名称（不含"男篮"后缀，供队名优先判据） */
    private static final String[] BASKETBALL_CLUBS = {
            "巴塞罗那", "皇家马德里", "奥林匹亚科斯", "帕纳辛纳科斯",
            "费内巴切", "阿纳多卢艾菲斯", "艾菲斯", "摩纳哥",
            "米兰阿玛尼", "博洛尼亚维图斯", "马卡比",
            "anadolu", "efes", "fenerbahce", "olympiacos", "panathinaikos",
            "real madrid", "barcelona", "monaco", "maccabi", "virtus bologna"
    };

    /** 明确的足球联赛名称（含中英） */
    private static final String[] FOOTBALL_LEAGUES = {
            "英超", "西甲", "德甲", "意甲", "法甲", "中超", "中甲", "中乙", "中冠",
            "欧冠", "欧联", "欧协联", "世界杯", "亚洲杯", "欧洲杯", "世俱杯",
            "premier league", "la liga", "serie a", "bundesliga", "ligue 1",
            "champions league", "europa league", "world cup"
    };

    private String detectSportType(String text) {
        String s = safe(text).toLowerCase(Locale.ROOT);

        // 1. 联赛名明确指示足球 → 直接返回 football（联赛权重大于队名）
        for (String league : FOOTBALL_LEAGUES) {
            if (s.contains(league.toLowerCase(Locale.ROOT))) {
                return "football";
            }
        }

        // 2. 强篮球关键词（含男篮/篮联/篮球/技术术语）
        String[] basketballStrong = {
                "篮球", "男篮", "女篮", "nba", "cba", "wnba", "basketball",
                "欧篮联", "篮联", "篮甲", "篮超", "土篮",
                "篮板", "三分", "罚球", "助攻", "盖帽", "控卫", "命中率",
                "费内巴切", "阿纳多卢艾菲斯", "艾菲斯",
                "奥林匹亚科斯", "帕纳辛纳科斯", "皇家马德里男篮", "巴塞罗那男篮",
                "摩纳哥男篮", "米兰阿玛尼", "博洛尼亚维图斯", "马卡比"
        };
        for (String w : basketballStrong) {
            if (s.contains(w.toLowerCase(Locale.ROOT))) return "basketball";
        }

        // 3. 已知篮球俱乐部名称（不含"男篮"后缀，队名优先）：只要 matchInfo 中出现这些队名，
        //    且没有被第1步的足球联赛驳回，就按篮球处理
        for (String club : BASKETBALL_CLUBS) {
            if (s.contains(club.toLowerCase(Locale.ROOT))) {
                return "basketball";
            }
        }

        // 4. 强足球关键词（不含联赛名，联赛已在第1步处理）
        String[] footballStrong = {
                "足球", "soccer", "football",
                "女足", "男足", "u16", "u17", "u18", "u19", "u20", "u21", "u22", "u23",
                "进球", "角球", "越位", "红牌", "黄牌", "点球", "任意球",
                "射门", "射正", "控球", "门将", "后卫", "中场", "前锋",
                "边路", "边锋", "禁区", "防线", "后防", "压迫", "反击"
        };
        for (String w : footballStrong) {
            if (s.contains(w.toLowerCase(Locale.ROOT))) return "football";
        }

        return "";
    }

    private String normalizeSportType(String value) {
        String s = safe(value).toLowerCase(Locale.ROOT);
        if (s.contains("basketball") || s.contains("篮球")) return "basketball";
        if (s.contains("football") || s.contains("soccer") || s.contains("足球")) return "football";
        return "";
    }

    private String buildManualResultInfo(String resultInfo, String finalScore) {
        StringBuilder sb = new StringBuilder();

        if (resultInfo != null && !resultInfo.trim().isEmpty()) {
            sb.append(resultInfo.trim());
        }

        if (finalScore != null && !finalScore.trim().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("最终比分：").append(finalScore.trim());
        }

        return sb.toString().trim();
    }

    private String getRelatedArticleContent(Long id) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT final_content FROM article_task WHERE id=? LIMIT 1");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("final_content");
            }
        } catch (Exception e) {
            return "";
        }

        return "";
    }
}
