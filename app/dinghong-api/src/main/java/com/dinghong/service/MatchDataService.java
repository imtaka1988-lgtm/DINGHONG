package com.dinghong.service;

import com.dinghong.entity.Injury;
import com.dinghong.entity.MarketMove;
import com.dinghong.entity.MatchStats;
import com.dinghong.repository.InjuryRepository;
import com.dinghong.repository.MarketMoveRepository;
import com.dinghong.repository.MatchStatsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchDataService {

    private final MatchStatsRepository statsRepo;
    private final MarketMoveRepository marketRepo;
    private final InjuryRepository injuryRepo;
    private final long cacheMillis;

    private final Map<Long, CacheItem> cache = new ConcurrentHashMap<>();

    public MatchDataService(MatchStatsRepository statsRepo,
                            MarketMoveRepository marketRepo,
                            InjuryRepository injuryRepo,
                            @Value("${stats.cache-minutes:5}") long cacheMinutes) {
        this.statsRepo = statsRepo;
        this.marketRepo = marketRepo;
        this.injuryRepo = injuryRepo;
        this.cacheMillis = cacheMinutes * 60 * 1000;
    }

    public Map<String, Object> getMatchData(long matchId) {
        CacheItem item = cache.get(matchId);
        long now = System.currentTimeMillis();
        if (item != null && (now - item.ts) < cacheMillis) {
            return item.data;
        }

        List<MatchStats> stats = statsRepo.findByMatchId(matchId);
        List<MarketMove> moves = marketRepo.findByMatchId(matchId);
        List<Injury> injuries = injuryRepo.findByMatchId(matchId);

        Map<String, Object> result = new HashMap<>();
        result.put("stats", stats);
        result.put("marketMoves", moves);
        result.put("injuries", injuries);
        result.put("ts", Instant.now().toString());

        cache.put(matchId, new CacheItem(now, result));
        return result;
    }

    private record CacheItem(long ts, Map<String, Object> data) {}
}