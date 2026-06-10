package com.dinghong.controller.admin;

import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

@RestController
public class AdminBatchController {

    private final DataSource dataSource;

    public AdminBatchController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostMapping("/admin/matches/finish-all")
    public String finishAll() {

        try (Connection conn = dataSource.getConnection()) {

            PreparedStatement ps =
                    conn.prepareStatement(
                            "UPDATE match_live SET live_status='FINISHED' WHERE live_status='AVAILABLE'"
                    );

            int rows = ps.executeUpdate();

            return "success:" + rows;

        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
}
