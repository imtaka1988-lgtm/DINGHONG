package com.dinghong.controller.editor;

import com.dinghong.service.ai.DeepSeekService;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;

@RestController
@RequestMapping("/editor")
public class ArticleRewriteController {

    private final DataSource dataSource;
    private final DeepSeekService deepSeekService;

    public ArticleRewriteController(DataSource dataSource,
                                    DeepSeekService deepSeekService) {
        this.dataSource = dataSource;
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/rewrite/{id}")
    public String rewrite(@PathVariable Long id,
                          @RequestParam String note) {

        try(Connection conn = dataSource.getConnection()) {

            PreparedStatement ps =
                conn.prepareStatement(
                    "select final_content from article_task where id=?"
                );

            ps.setLong(1,id);

            ResultSet rs = ps.executeQuery();

            if(!rs.next()){
                return "文章不存在";
            }

            String content = rs.getString("final_content");

            String prompt =
                    "下面是一篇文章：\n\n" +
                    content +
                    "\n\n编辑修改意见：\n" +
                    note +
                    "\n\n请根据意见重新创作全文。";

            String newContent =
                    deepSeekService.chat(
                            "你是顶红体育编辑。",
                            prompt
                    );

            PreparedStatement update =
                conn.prepareStatement(
                    "update article_task set final_content=?,rewrite_note=? where id=?"
                );

            update.setString(1,newContent);
            update.setString(2,note);
            update.setLong(3,id);

            update.executeUpdate();

            return "success";

        } catch(Exception e){
            return e.getMessage();
        }
    }
}
