package com.dinghong.controller.editor;

import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

@RestController
@RequestMapping("/editor")
public class ArticleApproveController {

    private final DataSource dataSource;

    public ArticleApproveController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostMapping("/approve/{id}")
    public String approve(@PathVariable Long id) {

        try(Connection conn = dataSource.getConnection()) {

            PreparedStatement ps = conn.prepareStatement(
                "UPDATE article_task SET status='APPROVED' WHERE id=?"
            );

            ps.setLong(1,id);

            ps.executeUpdate();

            return "success";

        } catch(Exception e) {
            return e.getMessage();
        }
    }
}
