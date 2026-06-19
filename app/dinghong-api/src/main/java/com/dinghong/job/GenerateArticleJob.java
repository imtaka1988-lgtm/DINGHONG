package com.dinghong.job;

import com.dinghong.repository.ArticleDraftRepository;
import com.dinghong.service.MatchDataService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GenerateArticleJob {

    private final MatchDataService matchDataService;
    private final ArticleDraftRepository draftRepository;

    public GenerateArticleJob(MatchDataService matchDataService,
                              ArticleDraftRepository draftRepository) {
        this.matchDataService = matchDataService;
        this.draftRepository = draftRepository;
    }

    /**
     * 每小时运行一次，扫描当天有比赛且未生成文章的 match_id 列表。
     * 简化演示：这里用固定列表 [1,2,3] 代替 DB 查询。
     */
    @Scheduled(cron = "0 5 * * * *")
    public void run() {
        List<Long> todayMatches = List.of(1L, 2L, 3L); // TODO: replace with DAO

        for (Long mid : todayMatches) {
            if (draftRepository.existsGeneratedForMatch(mid)) continue;

            try {
                // 调用分析引擎脚本
                Process proc = new ProcessBuilder("python", "-m", "analysis.analysis_engine", String.valueOf(mid)).start();
                String json = new BufferedReader(new InputStreamReader(proc.getInputStream()))
                        .lines().collect(Collectors.joining());
                proc.waitFor();
                if (json == null || json.trim().isEmpty()) continue;

                // 简单校验 json
                if (json.contains("\"match_id\"")) {
                    draftRepository.insertGenerated(mid, json);
                }
            } catch (Exception e) {
                System.err.println("[GenerateArticleJob] failed for match " + mid + ": " + e.getMessage());
            }
        }
    }
}