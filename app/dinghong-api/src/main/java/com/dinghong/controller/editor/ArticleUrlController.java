package com.dinghong.controller.editor;

import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

@RestController
@RequestMapping("/editor/articles")
public class ArticleUrlController {

    private final DataSource dataSource;

    public ArticleUrlController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PutMapping("/{id}/url")
    public String updateUrl(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String url = body.get("url");

        if (url == null || url.trim().isEmpty()) {
            return "error:文章链接不能为空";
        }

        url = url.trim();

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "error:文章链接必须以 http:// 或 https:// 开头";
        }

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE article_task SET article_url=? WHERE id=?"
            );
            ps.setString(1, url);
            ps.setLong(2, id);

            int rows = ps.executeUpdate();

            if (rows <= 0) {
                return "error:未找到文章";
            }

            return "success";

        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
}
