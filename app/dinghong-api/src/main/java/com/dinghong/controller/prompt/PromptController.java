package com.dinghong.controller.prompt;

import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/admin/prompts")
public class PromptController {

    private final DataSource dataSource;

    public PromptController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public List<Map<String, Object>> list() throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT id,prompt_code,prompt_name,prompt_content,status,updated_at FROM ai_prompt ORDER BY id ASC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getLong("id"));
                map.put("code", rs.getString("prompt_code"));
                map.put("name", rs.getString("prompt_name"));
                map.put("content", rs.getString("prompt_content"));
                map.put("status", rs.getString("status"));
                map.put("updatedAt", rs.getString("updated_at"));
                list.add(map);
            }
        }

        return list;
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @RequestBody Map<String, String> body) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "UPDATE ai_prompt SET prompt_name=?, prompt_content=?, status=? WHERE id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, body.get("name"));
            ps.setString(2, body.get("content"));
            ps.setString(3, body.get("status"));
            ps.setLong(4, id);
            ps.executeUpdate();
        }

        return "ok";
    }
}
