package com.dinghong.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("upload")
public class UploadProperties {

    private String dir;
    private String publicBaseUrl;
    private String liveQrDir;
    private String liveQrPublicPrefix;
    private String livePlayPrefix;

    public String getDir() { return dir; }
    public void setDir(String dir) { this.dir = dir; }
    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public String getLiveQrDir() { return liveQrDir; }
    public void setLiveQrDir(String liveQrDir) { this.liveQrDir = liveQrDir; }
    public String getLiveQrPublicPrefix() { return liveQrPublicPrefix; }
    public void setLiveQrPublicPrefix(String liveQrPublicPrefix) { this.liveQrPublicPrefix = liveQrPublicPrefix; }
    public String getLivePlayPrefix() { return livePlayPrefix; }
    public void setLivePlayPrefix(String livePlayPrefix) { this.livePlayPrefix = livePlayPrefix; }
}
