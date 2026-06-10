package com.dinghong.service.editor;

public class ArticleBlockedException extends RuntimeException {

    private final String code;
    private final String reason;

    public ArticleBlockedException(String code, String reason) {
        super(reason);
        this.code = code == null || code.trim().isEmpty() ? "article_blocked" : code.trim();
        this.reason = reason == null || reason.trim().isEmpty() ? "资料不足，禁止生成文章。" : reason.trim();
    }

    public String getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }
}
