package com.dinghong.entity;

/**
 * 球员伤停信息实体
 */
public record Injury(
        long id,
        long matchId,
        String team,
        String playerName,
        String position,
        String status,
        String note
) {
}