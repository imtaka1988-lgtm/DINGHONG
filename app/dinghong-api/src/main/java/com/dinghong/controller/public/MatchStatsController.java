package com.dinghong.controller.public;

import com.dinghong.service.MatchDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/match")
@CrossOrigin(origins = "*")
public class MatchStatsController {

    private final MatchDataService matchDataService;

    public MatchStatsController(MatchDataService matchDataService) {
        this.matchDataService = matchDataService;
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> stats(@PathVariable("id") long id) {
        Map<String, Object> data = matchDataService.getMatchData(id);
        return ResponseEntity.ok(data);
    }
}