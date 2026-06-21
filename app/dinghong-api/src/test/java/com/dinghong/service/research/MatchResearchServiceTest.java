package com.dinghong.service.research;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 赛果判断逻辑单元测试。
 * 覆盖：比分预测/预测比分/参考比分 → 不允许，最终比分/全场比分/赛果 → 允许。
 */
class MatchResearchServiceTest {

    private final MatchResearchService service = new MatchResearchService(null, null);

    @Test
    void shouldRejectPredictionScore() {
        // 比分预测 2-1 → 不允许人工赛果兜底
        String matchInfo = "德国 VS 芬兰 比分预测 2-1";
        boolean result = service.hasManualResultEvidencePublic(matchInfo);
        assertFalse(result, "比分预测 2-1 不允许走人工赛果兜底");
    }

    @Test
    void shouldRejectPredictedScore() {
        // 预测比分 2-1 → 不允许
        String matchInfo = "德国 VS 芬兰 预测比分 2-1";
        boolean result = service.hasManualResultEvidencePublic(matchInfo);
        assertFalse(result, "预测比分 2-1 不允许走人工赛果兜底");
    }

    @Test
    void shouldRejectReferenceScore() {
        // 参考比分 2-1 → 不允许
        String matchInfo = "德国 VS 芬兰 参考比分 2-1";
        boolean result = service.hasManualResultEvidencePublic(matchInfo);
        assertFalse(result, "参考比分 2-1 不允许走人工赛果兜底");
    }

    @Test
    void shouldAllowFinalScore() {
        // 最终比分 2-1 → 允许
        String matchInfo = "德国 VS 芬兰 最终比分 2-1";
        boolean result = service.hasManualResultEvidencePublic(matchInfo);
        assertTrue(result, "最终比分 2-1 允许走人工赛果兜底");
    }

    @Test
    void shouldAllowFullScore() {
        // 全场比分 2-1 → 允许
        String matchInfo = "德国 VS 芬兰 全场比分 2-1";
        boolean result = service.hasManualResultEvidencePublic(matchInfo);
        assertTrue(result, "全场比分 2-1 允许走人工赛果兜底");
    }

    @Test
    void shouldAllowResultKeyword() {
        // 赛果：2-1 → 允许
        String matchInfo = "德国 VS 芬兰 赛果：2-1";
        boolean result = service.hasManualResultEvidencePublic(matchInfo);
        assertTrue(result, "赛果：2-1 允许走人工赛果兜底");
    }

    @Test
    void shouldRejectEmptyMatchInfo() {
        assertFalse(service.hasManualResultEvidencePublic(null));
        assertFalse(service.hasManualResultEvidencePublic(""));
        assertFalse(service.hasManualResultEvidencePublic("   "));
    }
}
