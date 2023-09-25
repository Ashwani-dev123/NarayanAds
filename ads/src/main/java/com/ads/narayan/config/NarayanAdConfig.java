package com.ads.narayan.config;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

public class NarayanAdConfig {


    //switch mediation use for app
    public static final int PROVIDER_ADMOB = 0;
    public static final int PROVIDER_MAX = 1;


    public static final String ENVIRONMENT_DEVELOP = "develop";
    public static final String ENVIRONMENT_PRODUCTION = "production";

    public static final String DEFAULT_TOKEN_FACEBOOK_SDK = "client_token";


    private int mediationProvider = PROVIDER_ADMOB;

    private boolean isVariantDev = false;


    private AdjustConfig adjustConfig;


    private AppsflyerConfig appsflyerConfig;

    private String eventNamePurchase = "";
    private String idAdResume;
    private List<String> listDeviceTest = new ArrayList();

    private Application application;
    private boolean enableAdResume = false;
    private String facebookClientToken = DEFAULT_TOKEN_FACEBOOK_SDK;


    private int intervalInterstitialAd = 0;

    public NarayanAdConfig(Application application) {
        this.application = application;
    }

    public NarayanAdConfig(Application application, int mediationProvider, String environment) {
        this.mediationProvider = mediationProvider;
        this.isVariantDev = environment.equals(ENVIRONMENT_DEVELOP);
        this.application = application;
    }


    public void setMediationProvider(int mediationProvider) {
        this.mediationProvider = mediationProvider;
    }


    @Deprecated
    public void setVariant(Boolean isVariantDev) {
        this.isVariantDev = isVariantDev;
    }

    public void setEnvironment(String environment) {
        this.isVariantDev = environment.equals(ENVIRONMENT_DEVELOP);
    }

    public AdjustConfig getAdjustConfig() {
        return adjustConfig;
    }

    public void setAdjustConfig(AdjustConfig adjustConfig) {
        this.adjustConfig = adjustConfig;
    }

    public AppsflyerConfig getAppsflyerConfig() {
        return appsflyerConfig;
    }

    public void setAppsflyerConfig(AppsflyerConfig appsflyerConfig) {
        this.appsflyerConfig = appsflyerConfig;
    }

    public String getEventNamePurchase() {
        return eventNamePurchase;
    }

    public Application getApplication() {
        return application;
    }


    public int getMediationProvider() {
        return mediationProvider;
    }

    public Boolean isVariantDev() {
        return isVariantDev;
    }


    public String getIdAdResume() {
        return idAdResume;
    }

    public List<String> getListDeviceTest() {
        return listDeviceTest;
    }

    public void setListDeviceTest(List<String> listDeviceTest) {
        this.listDeviceTest = listDeviceTest;
    }


    public void setIdAdResume(String idAdResume) {
        this.idAdResume = idAdResume;
        enableAdResume = true;
    }

    public Boolean isEnableAdResume() {
        return enableAdResume;
    }


    public Boolean isEnableAdjust() {
        if (adjustConfig == null)
            return false;
        return adjustConfig.isEnableAdjust();
    }

    public boolean isEnableAppsflyer() {
        if (appsflyerConfig == null)
            return false;
        return appsflyerConfig.isEnableAppsflyer();
    }

    public int getIntervalInterstitialAd() {
        return intervalInterstitialAd;
    }

    public void setIntervalInterstitialAd(int intervalInterstitialAd) {
        this.intervalInterstitialAd = intervalInterstitialAd;
    }

    public void setFacebookClientToken(String token) {
        this.facebookClientToken = token;
    }

    public String getFacebookClientToken() {
        return this.facebookClientToken;
    }
}
