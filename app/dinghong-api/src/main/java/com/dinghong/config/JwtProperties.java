package com.dinghong.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs = 86400000; // default 24h

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
}
