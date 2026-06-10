package com.dinghong.controller.editor;

import com.dinghong.service.research.MatchResearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/editor")
public class ResearchTestController {

    private final MatchResearchService matchResearchService;

    public ResearchTestController(MatchResearchService matchResearchService) {
        this.matchResearchService = matchResearchService;
    }

    @GetMapping("/research-test")
    public String test(@RequestParam String matchInfo) {
        return matchResearchService.research(matchInfo);
    }
}
