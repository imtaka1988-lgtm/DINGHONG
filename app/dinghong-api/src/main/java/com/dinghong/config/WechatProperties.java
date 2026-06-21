package com.dinghong.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("wechat")
public class WechatProperties {

    private String verifyToken;
    private String appid;
    private String secret;

    public String getVerifyToken() { return verifyToken; }
    public void setVerifyToken(String verifyToken) { this.verifyToken = verifyToken; }
    public String getAppid() { return appid; }
    public void setAppid(String appid) { this.appid = appid; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
