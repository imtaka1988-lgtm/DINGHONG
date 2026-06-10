package com.dinghong.service.research;

public class ReviewResearchResult {

    private final String matchInfo;
    private final String category;
    private final boolean blocked;
    private final String blockedReason;
    private final boolean hasResultEvidence;
    private final String source;
    private final String material;
    private final String raw;

    private ReviewResearchResult(String matchInfo,
                                 String category,
                                 boolean blocked,
                                 String blockedReason,
                                 boolean hasResultEvidence,
                                 String source,
                                 String material,
                                 String raw) {
        this.matchInfo = matchInfo;
        this.category = category;
        this.blocked = blocked;
        this.blockedReason = blockedReason;
        this.hasResultEvidence = hasResultEvidence;
        this.source = source;
        this.material = material;
        this.raw = raw;
    }

    public static ReviewResearchResult blocked(String matchInfo, String reason, String raw) {
        return new ReviewResearchResult(
                matchInfo,
                "REVIEW",
                true,
                reason == null || reason.trim().isEmpty() ? "未获取到明确赛后资料" : reason.trim(),
                false,
                "BAIDU_SEARCH",
                "",
                raw == null ? "" : raw
        );
    }

    public static ReviewResearchResult passed(String matchInfo, String material, String raw) {
        return new ReviewResearchResult(
                matchInfo,
                "REVIEW",
                false,
                "",
                true,
                "BAIDU_SEARCH+DEEPSEEK整理",
                material == null ? "" : material.trim(),
                raw == null ? "" : raw
        );
    }

    public String getMatchInfo() {
        return matchInfo;
    }

    public String getCategory() {
        return category;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public boolean isHasResultEvidence() {
        return hasResultEvidence;
    }

    public String getSource() {
        return source;
    }

    public String getMaterial() {
        return material;
    }

    public String getRaw() {
        return raw;
    }
}
