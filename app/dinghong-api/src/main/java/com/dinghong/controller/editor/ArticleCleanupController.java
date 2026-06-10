package com.dinghong.controller.editor;

import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@RestController
@RequestMapping("/editor")
public class ArticleCleanupController {

    private final DataSource dataSource;

    public ArticleCleanupController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostMapping("/cleanup")
    public String cleanup() {

        try(Connection conn = dataSource.getConnection()) {

            Statement st = conn.createStatement();

            st.executeUpdate(
                "UPDATE article_task SET status='ARCHIVED' " +
                "WHERE article_category='PREVIEW' " +
                "AND id NOT IN (" +
                "SELECT id FROM (" +
                "SELECT id FROM article_task " +
                "WHERE article_category='PREVIEW' " +
                "ORDER BY id DESC LIMIT 1" +
                ") t)"
            );

            st.executeUpdate(
                "UPDATE article_task SET status='ARCHIVED' " +
                "WHERE article_category='REVIEW' " +
                "AND id NOT IN (" +
                "SELECT id FROM (" +
                "SELECT id FROM article_task " +
                "WHERE article_category='REVIEW' " +
                "ORDER BY id DESC LIMIT 1" +
                ") t)"
            );

            return "success";

        } catch(Exception e) {
            return "error:" + e.getMessage();
        }
    }
}
