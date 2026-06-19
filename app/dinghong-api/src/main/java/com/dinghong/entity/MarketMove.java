package com.dinghong.entity;

import java.time.Instant;

/**
 * 盘口 / 赔率变化实体
 */
public record MarketMove(
        long id,
        long matchId,
        String market,
        String bookmaker,
        Double openPriceHome,
        Double openPriceAway,
        Double openPointHome,
        Double openPointAway,
        Double livePriceHome,
        Double livePriceAway,
        Double livePointHome,
        Double livePointAway,
        Instant lastUpdate
) {
}