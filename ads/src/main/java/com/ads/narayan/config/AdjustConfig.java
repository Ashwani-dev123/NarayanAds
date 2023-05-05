package com.ads.narayan.config;


public class AdjustConfig {

    private boolean enableAdjust = false;


    private String adjustToken = "";


    private String eventNamePurchase = "";


    private String eventAdImpression = "";

    public AdjustConfig(boolean enableAdjust) {
        this.enableAdjust = enableAdjust;
    }

    public AdjustConfig(boolean enableAdjust, String adjustToken) {
        this.enableAdjust = enableAdjust;
        this.adjustToken = adjustToken;
    }

    public boolean isEnableAdjust() {
        return enableAdjust;
    }

    public void setEnableAdjust(boolean enableAdjust) {
        this.enableAdjust = enableAdjust;
    }

    public String getAdjustToken() {
        return adjustToken;
    }

    public void setAdjustToken(String adjustToken) {
        this.adjustToken = adjustToken;
    }

    public String getEventNamePurchase() {
        return eventNamePurchase;
    }

    public void setEventNamePurchase(String eventNamePurchase) {
        this.eventNamePurchase = eventNamePurchase;
    }

    public String getEventAdImpression() {
        return eventAdImpression;
    }

    public void setEventAdImpression(String eventAdImpression) {
        this.eventAdImpression = eventAdImpression;
    }
}
