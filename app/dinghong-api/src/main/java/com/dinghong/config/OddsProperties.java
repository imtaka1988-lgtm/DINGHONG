package com.dinghong.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("odds.api")
public class OddsProperties {

    private boolean enabled = false;
    private String key;
    private String regions = "eu,uk,us";
    private String markets = "h2h,spreads,totals";
    private String oddsFormat = "decimal";
    private String footballSports;
    private String basketballSports;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getRegions() { return regions; }
    public void setRegions(String regions) { this.regions = regions; }
    public String getMarkets() { return markets; }
    public void setMarkets(String markets) { this.markets = markets; }
    public String getOddsFormat() { return oddsFormat; }
    public void setOddsFormat(String oddsFormat) { this.oddsFormat = oddsFormat; }
    public String getFootballSports() { return footballSports; }
    public void setFootballSports(String footballSports) { this.footballSports = footballSports; }
    public String getBasketballSports() { return basketballSports; }
    public void setBasketballSports(String basketballSports) { this.basketballSports = basketballSports; }
}
