package com.dinghong.controller.editor;

import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/editor/articles")
public class ArticleAdminController {

    private final DataSource dataSource;

    public ArticleAdminController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "false") boolean archived) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            String sql;

            if (archived) {
                sql = "SELECT id,title,article_type,status,author_editor,article_category,related_article_id,created_at " +
                        "FROM article_task WHERE status='ARCHIVED' ORDER BY id DESC";
            } else {
                sql = "SELECT id,title,article_type,status,author_editor,article_category,related_article_id,created_at " +
                        "FROM article_task WHERE status <> 'ARCHIVED' ORDER BY id DESC";
            }

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getLong("id"));
                map.put("title", rs.getString("title"));
                map.put("type", rs.getString("article_type"));
                map.put("status", rs.getString("status"));
                map.put("author", rs.getString("author_editor"));
                map.put("category", rs.getString("article_category"));
                map.put("relatedArticleId", rs.getLong("related_article_id"));
                map.put("createdAt", rs.getString("created_at"));
                list.add(map);
            }
        }

        return list;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) throws Exception {
        Map<String, Object> map = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT * FROM article_task WHERE id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                map.put("id", rs.getLong("id"));
                map.put("title", rs.getString("title"));
                map.put("type", rs.getString("article_type"));
                map.put("status", rs.getString("status"));
                map.put("author", rs.getString("author_editor"));
                map.put("category", rs.getString("article_category"));
                map.put("relatedArticleId", rs.getLong("related_article_id"));
                map.put("finalContent", rs.getString("final_content"));
                map.put("wechatTitle", rs.getString("wechat_title"));
                map.put("wechatSummary", rs.getString("wechat_summary"));
                map.put("articleUrl", rs.getString("article_url"));
                map.put("coverText", rs.getString("cover_text"));
                map.put("createdAt", rs.getString("created_at"));
            }
        }

        return map;
    }
}
