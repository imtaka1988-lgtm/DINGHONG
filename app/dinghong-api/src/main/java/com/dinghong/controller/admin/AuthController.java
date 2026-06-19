package com.dinghong.controller.admin;

import com.dinghong.config.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final String adminUser;
    private final String adminPass;

    public AuthController(JwtUtil jwtUtil,
                          @Value("${admin.user:admin}") String adminUser,
                          @Value("${admin.pass:DingHong2026}") String adminPass) {
        this.jwtUtil = jwtUtil;
        this.adminUser = adminUser;
        this.adminPass = adminPass;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestParam String username,
                                                      @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();

        if (adminUser.equals(username) && adminPass.equals(password)) {
            String token = jwtUtil.generateToken(username);
            result.put("success", true);
            result.put("token", token);
            result.put("message", "登录成功");
            return ResponseEntity.ok(result);
        }

        result.put("success", false);
        result.put("message", "账号或密码错误");
        return ResponseEntity.status(401).body(result);
    }

    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Object>> checkAuth() {
        // 能到达这里说明 JwtAuthFilter 已通过
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已认证");
        return ResponseEntity.ok(result);
    }
}
