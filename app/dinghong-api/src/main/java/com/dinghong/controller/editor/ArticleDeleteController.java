package com.dinghong.controller.editor;

import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

@RestController
@RequestMapping("/editor")
public class ArticleDeleteController {

    private final DataSource dataSource;

    public ArticleDeleteController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostMapping("/delete/{id}")
    public String deleteOne(@PathVariable Long id) {
        try(Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM article_task WHERE id=?");
            ps.setLong(1, id);
            ps.executeUpdate();
            return "success";
        } catch(Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/delete-before-today")
    public String deleteBeforeToday() {
        try(Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM article_task WHERE DATE(created_at) < CURDATE()"
            );
            int rows = ps.executeUpdate();
            return "success:" + rows;
        } catch(Exception e) {
            return "error:" + e.getMessage();
        }
    }
}
