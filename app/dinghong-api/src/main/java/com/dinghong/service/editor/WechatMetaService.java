package com.dinghong.service.editor;

import com.dinghong.service.ai.DeepSeekService;
import org.springframework.stereotype.Service;

@Service
public class WechatMetaService {

    private final DeepSeekService deepSeekService;

    public WechatMetaService(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    public String generateTitle(String articleContent) {
        String prompt =
                "请根据下面这篇顶红体育文章，生成一个适合微信公众号的标题。\n\n" +
                "要求：\n" +
                "1. 20字以内。\n" +
                "2. 中文。\n" +
                "3. 有点击欲望，但不要标题党。\n" +
                "4. 不要出现必中、包红、稳赚、重注等词。\n" +
                "5. 只返回标题，不要解释。\n\n" +
                articleContent;

        return clean(deepSeekService.chat("你是顶红体育公众号标题编辑。", prompt), 250);
    }

    public String generateSummary(String articleContent) {
        String prompt =
                "请根据下面这篇顶红体育文章，生成微信公众号摘要。\n\n" +
                "要求：\n" +
                "1. 80字以内。\n" +
                "2. 中文。\n" +
                "3. 说清楚比赛、作者观点和看法。\n" +
                "4. 不要出现必中、包红、稳赚、重注等词。\n" +
                "5. 只返回摘要，不要解释。\n\n" +
                articleContent;

        return clean(deepSeekService.chat("你是顶红体育公众号摘要编辑。", prompt), 450);
    }

    public String generateCoverHeadline(String articleContent) {
        String prompt =
                "请根据下面这篇顶红体育文章，提炼一句适合公众号封面使用的核心观点。\n\n" +
                "要求：\n" +
                "1. 4到8个字。\n" +
                "2. 不要球队名。\n" +
                "3. 不要比赛名。\n" +
                "4. 不要标点符号。\n" +
                "5. 不要解释。\n" +
                "6. 只返回一句短观点。\n\n" +
                "示例：\n" +
                "大热不稳\n" +
                "暗藏冷门\n" +
                "客场有戏\n" +
                "这球难踢\n\n" +
                articleContent;

        return cleanShort(deepSeekService.chat("你是顶红体育公众号封面文案编辑。", prompt));
    }

    public String generateCoverText(String matchInfo, String author, String category) {
        String type = "REVIEW".equals(category) ? "赛后复盘" : "赛前预测";
        String name = authorName(author);

        if ("REVIEW".equals(category)) {
            return name + "｜复盘结论｜" + matchInfo;
        }

        return name + "｜今日观点｜" + matchInfo;
    }

    private String authorName(String author) {
        if ("laozhou".equals(author)) return "老周";
        if ("akai".equals(author)) return "阿凯";
        if ("laotang".equals(author)) return "老唐";
        if ("xiaobei".equals(author)) return "小北";
        return "顶红体育";
    }

    private String clean(String s, int max) {
        if (s == null) return "";
        s = s.replace("\"", "")
             .replace("《", "")
             .replace("》", "")
             .replace("\n", "")
             .replace("\r", "")
             .replaceAll("[（(]\\d{1,3}字[）)]", "")
             .trim();

        if (s.length() > max) {
            s = s.substring(0, max);
        }

        return s;
    }

    private String cleanShort(String s) {
        if (s == null) return "今日观点";

        s = s.replace("\"", "")
             .replace("《", "")
             .replace("》", "")
             .replace("。", "")
             .replace("，", "")
             .replace(",", "")
             .replace("：", "")
             .replace(":", "")
             .replace("！", "")
             .replace("?", "")
             .replace("？", "")
             .replace("\n", "")
             .replace("\r", "")
             .trim();

        if (s.length() > 8) {
            s = s.substring(0, 8);
        }

        if (s.length() == 0) {
            return "今日观点";
        }

        return s;
    }
}
