package com.dinghong.controller.editor;

import com.dinghong.service.odds.OddsFetchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OddsTestController {

    private final OddsFetchService oddsFetchService;

    public OddsTestController(OddsFetchService oddsFetchService) {
        this.oddsFetchService = oddsFetchService;
    }

    @GetMapping("/editor/odds-test")
    public String oddsTest(@RequestParam String matchInfo) {
        return oddsFetchService.fetchOdds(matchInfo);
    }

    @GetMapping("/editor/odds-sports")
    public String oddsSports() {
        return oddsFetchService.listSports();
    }

    @GetMapping("/editor/odds-events")
    public String oddsEvents() {
        return oddsFetchService.listCurrentEvents();
    }
}
