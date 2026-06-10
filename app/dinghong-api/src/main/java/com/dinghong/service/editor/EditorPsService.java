package com.dinghong.service.editor;

import com.dinghong.service.ai.DeepSeekService;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;

@Service
public class EditorPsService {

    private final DataSource dataSource;
    private final DeepSeekService deepSeekService;
    private final Random random = new Random();

    public EditorPsService(DataSource dataSource, DeepSeekService deepSeekService) {
        this.dataSource = dataSource;
        this.deepSeekService = deepSeekService;
    }

    public String randomPs(String author, String articleContent) {

        String psEditor = pickPsEditor(author);
        String promptCode = getPromptCode(psEditor);
        String systemPrompt = getPrompt(promptCode);

        String userPrompt =
                "下面是一篇顶红体育编辑部文章：\n\n" +
                articleContent + "\n\n" +
                "请你以" + editorName(psEditor) + "的身份，给这篇文章写一句PS吐槽。\n\n" +
                "要求：\n" +
                "1. 只写一句话。\n" +
                "2. 20到50字。\n" +
                "3. 像真实编辑部同事之间的调侃。\n" +
                "4. 不要写分析报告。\n" +
                "5. 不要出现AI感。\n" +
                "6. 只能出现老周、阿凯、老唐、小北这四个人。\n" +
                "7. 不要虚构其他人物。\n" +
                "8. 不要写标题。\n";

        String ps = deepSeekService.chat(systemPrompt, userPrompt);

        ps = ps.replace("PS：", "")
               .replace("PS:", "")
               .replace(editorName(psEditor) + "：", "")
               .replace(editorName(psEditor) + ":", "")
               .replace("###", "")
               .replace("##", "")
               .replace("#", "")
               .replace("***", "")
               .replace("**", "")
               .replace("*", "")
               .replace("```", "")
               .replace("稳如狗", "有点上头")
               .replace("稳赢", "看得太满")
               .replace("稳胆", "看得太满")
               .replace("必中", "别说太满")
               .replace("包红", "别说太满")
               .replace("立帖为证", "")
               .replace("跟单", "参考")
               .replace("上车", "关注")
               .replace("收米", "")
               .trim();

        return "\n\nPS：\n\n" + editorName(psEditor) + "：\n" + ps;
    }

    private String pickPsEditor(String author) {
        String[] all = {"laozhou", "akai", "laotang", "xiaobei"};

        for (int i = 0; i < 10; i++) {
            String p = all[random.nextInt(all.length)];
            if (!p.equals(author)) {
                return p;
            }
        }

        return "laozhou";
    }

    private String getPromptCode(String author) {
        if ("laozhou".equals(author)) return "chief_editor";
        if ("akai".equals(author)) return "basketball_editor";
        if ("laotang".equals(author)) return "football_editor";
        if ("xiaobei".equals(author)) return "analyst_editor";
        return "chief_editor";
    }

    private String editorName(String code) {
        if ("laozhou".equals(code)) return "老周";
        if ("akai".equals(code)) return "阿凯";
        if ("laotang".equals(code)) return "老唐";
        if ("xiaobei".equals(code)) return "小北";
        return code;
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
            return "你是顶红体育编辑，请用真实人物口吻说话。";
        }

        return "你是顶红体育编辑，请用真实人物口吻说话。";
    }
}
