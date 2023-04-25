package com.ads.narayan.config;


public class AppsflyerConfig {

    private boolean enableAppsflyer = false;


    private String appsflyerToken = "";


    private String eventAdImpression = "";

    public AppsflyerConfig(boolean enableAppsflyer) {
        this.enableAppsflyer = enableAppsflyer;
    }

    public AppsflyerConfig(boolean enableAppsflyer, String appsflyerToken) {
        this.enableAppsflyer = enableAppsflyer;
        this.appsflyerToken = appsflyerToken;
    }

    public boolean isEnableAppsflyer() {
        return enableAppsflyer;
    }

    public void setEnableAppsflyer(boolean enableAppsflyer) {
        this.enableAppsflyer = enableAppsflyer;
    }

    public String getAppsflyerToken() {
        return appsflyerToken;
    }

    public void setAppsflyerToken(String appsflyerToken) {
        this.appsflyerToken = appsflyerToken;
    }


    public String getEventAdImpression() {
        return eventAdImpression;
    }

    public void setEventAdImpression(String eventAdImpression) {
        this.eventAdImpression = eventAdImpression;
    }
    
}
