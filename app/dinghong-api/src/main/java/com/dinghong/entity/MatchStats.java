package com.dinghong.entity;

/**
 * 比赛近况与效率统计实体。
 */
public record MatchStats(
        long id,
        long matchId,
        String league,
        String season,
        String homeTeam,
        String awayTeam,
        double homeRating,
        double awayRating,
        double homeAttackEff,
        double awayAttackEff,
        double homeDefenseEff,
        double awayDefenseEff,
        int homeLast5Win,
        int awayLast5Win
) {
}