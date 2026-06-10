package com.dinghong.service.editor;

public class ReviewBlockedException extends RuntimeException {

    private final String reason;

    public ReviewBlockedException(String reason) {
        super(reason);
        this.reason = reason == null || reason.trim().isEmpty() ? "未获取到明确赛后资料" : reason.trim();
    }

    public String getReason() {
        return reason;
    }
}
