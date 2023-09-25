package com.ads.narayan.ads;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAttribution;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEventFailure;
import com.adjust.sdk.AdjustEventSuccess;
import com.adjust.sdk.AdjustSessionFailure;
import com.adjust.sdk.AdjustSessionSuccess;
import com.adjust.sdk.LogLevel;
import com.adjust.sdk.OnAttributionChangedListener;
import com.adjust.sdk.OnEventTrackingFailedListener;
import com.adjust.sdk.OnEventTrackingSucceededListener;
import com.adjust.sdk.OnSessionTrackingFailedListener;
import com.adjust.sdk.OnSessionTrackingSucceededListener;
import com.ads.narayan.R;
import com.ads.narayan.admob.Admob;
import com.ads.narayan.admob.AppOpenManager;
import com.ads.narayan.ads.nativeAds.NarayanAdAdapter;
import com.ads.narayan.ads.nativeAds.NarayanAdPlacer;
import com.ads.narayan.ads.wrapper.NarayanAdError;
import com.ads.narayan.ads.wrapper.NarayanAdValue;
import com.ads.narayan.ads.wrapper.NarayanInterstitialAd;
import com.ads.narayan.ads.wrapper.NarayanNativeAd;
import com.ads.narayan.ads.wrapper.NarayanRewardAd;
import com.ads.narayan.ads.wrapper.NarayanRewardItem;
import com.ads.narayan.applovin.AppLovin;
import com.ads.narayan.applovin.AppLovinCallback;
import com.ads.narayan.applovin.AppOpenMax;
import com.ads.narayan.billing.AppPurchase;
import com.ads.narayan.config.NarayanAdConfig;
import com.ads.narayan.event.NarayanAdjust;
import com.ads.narayan.event.NarayanAppsflyer;
import com.ads.narayan.event.NarayanLogEventManager;
import com.ads.narayan.funtion.AdCallback;
import com.ads.narayan.funtion.RewardCallback;
import com.ads.narayan.util.AppUtil;
import com.ads.narayan.util.SharePreferenceUtils;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.mediation.nativeAds.adPlacer.MaxAdPlacer;
import com.applovin.mediation.nativeAds.adPlacer.MaxRecyclerAdapter;
import com.facebook.FacebookSdk;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;

public class NarayanAd {
    public static final String TAG_ADJUST = "NarayanAdjust";
    public static final String TAG = "NarayanAd";
    private static volatile NarayanAd INSTANCE;
    private NarayanAdConfig adConfig;
    private NarayanInitCallback initCallback;
    private Boolean initAdSuccess = false;

    private boolean isInterAdsNormalLoaded = false;
    private boolean isInterAdsHighFloorLoaded = false;

    public static synchronized NarayanAd getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NarayanAd();
        }
        return INSTANCE;
    }

    /**
     * Set count click to show ads interstitial when call showInterstitialAdByTimes()
     *
     * @param countClickToShowAds - default = 3
     */
    public void setCountClickToShowAds(int countClickToShowAds) {
        Admob.getInstance().setNumToShowAds(countClickToShowAds);
        AppLovin.getInstance().setNumShowAds(countClickToShowAds);
    }

    /**
     * Set count click to show ads interstitial when call showInterstitialAdByTimes()
     *
     * @param countClickToShowAds Default value = 3
     * @param currentClicked      Default value = 0
     */
    public void setCountClickToShowAds(int countClickToShowAds, int currentClicked) {
        Admob.getInstance().setNumToShowAds(countClickToShowAds, currentClicked);
        AppLovin.getInstance().setNumToShowAds(countClickToShowAds, currentClicked);
    }


    /**
     * @param context
     * @param adConfig NarayanAdConfig object used for SDK initialisation
     */
    public void init(Application context, NarayanAdConfig adConfig) {
        init(context, adConfig, false);
    }

    /**
     * @param context
     * @param adConfig             NarayanAdConfig object used for SDK initialisation
     * @param enableDebugMediation set show Mediation Debugger - use only for Max Mediation
     */
    public void init(Application context, NarayanAdConfig adConfig, Boolean enableDebugMediation) {
        if (adConfig == null) {
            throw new RuntimeException("cant not set NarayanAdConfig null");
        }
        this.adConfig = adConfig;
        AppUtil.VARIANT_DEV = adConfig.isVariantDev();
        Log.i(TAG, "Config variant dev: " + AppUtil.VARIANT_DEV);
        if (adConfig.isEnableAppsflyer()) {
            Log.i(TAG, "init appsflyer");
            NarayanAppsflyer.enableAppsflyer = true;
            NarayanAppsflyer.getInstance().init(context, adConfig.getAppsflyerConfig().getAppsflyerToken(), this.adConfig.isVariantDev());
        }
        if (adConfig.isEnableAdjust()) {
            Log.i(TAG, "init adjust");
            NarayanAdjust.enableAdjust = true;
            setupAdjust(adConfig.isVariantDev(), adConfig.getAdjustConfig().getAdjustToken());
        }
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().init(context, new AppLovinCallback() {
                    @Override
                    public void initAppLovinSuccess() {
                        super.initAppLovinSuccess();
                        initAdSuccess = true;
                        if (initCallback != null)
                            initCallback.initAdSuccess();
                        if (adConfig.isEnableAdResume()) {
                            AppOpenMax.getInstance().init(adConfig.getApplication(), adConfig.getIdAdResume());
                        }
                    }
                }, enableDebugMediation);
                break;
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().init(context, adConfig.getListDeviceTest());
                if (adConfig.isEnableAdResume())
                    AppOpenManager.getInstance().init(adConfig.getApplication(), adConfig.getIdAdResume());

                initAdSuccess = true;
                if (initCallback != null)
                    initCallback.initAdSuccess();
                break;
        }
        FacebookSdk.setClientToken(adConfig.getFacebookClientToken());
        FacebookSdk.sdkInitialize(context);
    }

    public int getMediationProvider() {
        return adConfig.getMediationProvider();
    }

    public void setInitCallback(NarayanInitCallback initCallback) {
        this.initCallback = initCallback;
        if (initAdSuccess)
            initCallback.initAdSuccess();
    }

    private void setupAdjust(Boolean buildDebug, String adjustToken) {

        String environment = buildDebug ? AdjustConfig.ENVIRONMENT_SANDBOX : AdjustConfig.ENVIRONMENT_PRODUCTION;
        Log.i("Application", "setupAdjust: " + environment);
        AdjustConfig config = new AdjustConfig(adConfig.getApplication(), adjustToken, environment);

        // Change the log level.
        config.setLogLevel(LogLevel.VERBOSE);
        config.setPreinstallTrackingEnabled(true);
        config.setOnAttributionChangedListener(new OnAttributionChangedListener() {
            @Override
            public void onAttributionChanged(AdjustAttribution attribution) {
                Log.e(TAG_ADJUST, "Attribution callback called!");
                Log.e(TAG_ADJUST, "Attribution: " + attribution.toString());
            }
        });

        // Set event success tracking delegate.
        config.setOnEventTrackingSucceededListener(new OnEventTrackingSucceededListener() {
            @Override
            public void onFinishedEventTrackingSucceeded(AdjustEventSuccess eventSuccessResponseData) {
                Log.e(TAG_ADJUST, "Event success callback called!");
                Log.e(TAG_ADJUST, "Event success data: " + eventSuccessResponseData.toString());
            }
        });
        // Set event failure tracking delegate.
        config.setOnEventTrackingFailedListener(new OnEventTrackingFailedListener() {
            @Override
            public void onFinishedEventTrackingFailed(AdjustEventFailure eventFailureResponseData) {
                Log.e(TAG_ADJUST, "Event failure callback called!");
                Log.e(TAG_ADJUST, "Event failure data: " + eventFailureResponseData.toString());
            }
        });

        // Set session success tracking delegate.
        config.setOnSessionTrackingSucceededListener(new OnSessionTrackingSucceededListener() {
            @Override
            public void onFinishedSessionTrackingSucceeded(AdjustSessionSuccess sessionSuccessResponseData) {
                Log.e(TAG_ADJUST, "Session success callback called!");
                Log.e(TAG_ADJUST, "Session success data: " + sessionSuccessResponseData.toString());
            }
        });

        // Set session failure tracking delegate.
        config.setOnSessionTrackingFailedListener(new OnSessionTrackingFailedListener() {
            @Override
            public void onFinishedSessionTrackingFailed(AdjustSessionFailure sessionFailureResponseData) {
                Log.e(TAG_ADJUST, "Session failure callback called!");
                Log.e(TAG_ADJUST, "Session failure data: " + sessionFailureResponseData.toString());
            }
        });


        config.setSendInBackground(true);
        Adjust.onCreate(config);
        adConfig.getApplication().registerActivityLifecycleCallbacks(new AdjustLifecycleCallbacks());
    }

    private static final class AdjustLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityResumed(Activity activity) {
            Adjust.onResume();
        }

        @Override
        public void onActivityPaused(Activity activity) {
            Adjust.onPause();
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }
    }

    public NarayanAdConfig getAdConfig() {
        return adConfig;
    }

    public void loadBanner(final Activity mActivity, String id) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().loadBanner(mActivity, id);
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadBanner(mActivity, id);
        }
    }

    public void loadBanner(final Activity mActivity, String id, final NarayanAdCallback adCallback) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().loadBanner(mActivity, id, new AdCallback() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        adCallback.onAdLoaded();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        adCallback.onAdClicked();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        adCallback.onAdFailedToLoad(new NarayanAdError(i));
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        adCallback.onAdImpression();
                    }
                });
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadBanner(mActivity, id, new AdCallback() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        adCallback.onAdLoaded();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        adCallback.onAdClicked();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        adCallback.onAdFailedToLoad(new NarayanAdError(i));
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        adCallback.onAdImpression();
                    }
                });
        }
    }

    public void loadCollapsibleBanner(final Activity activity, String id, String gravity, AdCallback adCallback) {
        Admob.getInstance().loadCollapsibleBanner(activity, id, gravity, adCallback);
    }

    public void loadBannerFragment(final Activity mActivity, String id, final View rootView) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().loadBannerFragment(mActivity, id, rootView);
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadBannerFragment(mActivity, id, rootView);
        }
    }

    public void loadBannerFragment(final Activity mActivity, String id, final View rootView, final AdCallback adCallback) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().loadBannerFragment(mActivity, id, rootView, adCallback);
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadBannerFragment(mActivity, id, rootView, adCallback);
        }
    }

    public void loadCollapsibleBannerFragment(final Activity mActivity, String id, final View rootView, String gravity, AdCallback adCallback) {
        Admob.getInstance().loadCollapsibleBannerFragment(mActivity, id, rootView, gravity, adCallback);
    }

//    public void loadBanner(final Activity mActivity, String id, final NarayanAdCallback callback) {
//        switch (adConfig.getMediationProvider()) {
//            case NarayanAdConfig.PROVIDER_ADMOB:
//                Admob.getInstance().loadBanner(mActivity, id , new AdCallback(){
//                    @Override
//                    public void onAdClicked() {
//                        super.onAdClicked();
//                        callback.onAdClicked();
//                    }
//                });
//                break;
//            case NarayanAdConfig.PROVIDER_MAX:
//                AppLovin.getInstance().loadBanner(mActivity, id, new AppLovinCallback(){
//
//                });
//        }
//    }

    public void loadSplashInterstitialAds(final Context context, String id, long timeOut, long timeDelay, NarayanAdCallback adListener) {
        loadSplashInterstitialAds(context, id, timeOut, timeDelay, true, adListener);
    }

    public void loadSplashInterstitialAds(final Context context, String id, long timeOut, long timeDelay, boolean showSplashIfReady, NarayanAdCallback adListener) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().loadSplashInterstitialAds(context, id, timeOut, timeDelay, showSplashIfReady, new AdCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        adListener.onAdClosed();
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        adListener.onAdFailedToLoad(new NarayanAdError(i));

                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        adListener.onAdFailedToShow(new NarayanAdError(adError));

                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        adListener.onAdLoaded();
                    }

                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        adListener.onAdSplashReady();
                    }


                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (adListener != null) {
                            adListener.onAdClicked();
                        }
                    }
                });
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadSplashInterstitialAds(context, id, timeOut, timeDelay, showSplashIfReady, new AppLovinCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        adListener.onAdClosed();
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable MaxError i) {
                        super.onAdFailedToLoad(i);
                        adListener.onAdFailedToLoad(new NarayanAdError(i));
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable MaxError adError) {
                        super.onAdFailedToShow(adError);
                        adListener.onAdFailedToShow(new NarayanAdError(adError));
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        adListener.onAdLoaded();
                    }

                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        adListener.onAdSplashReady();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (adListener != null) {
                            adListener.onAdClicked();
                        }
                    }
                });
        }
    }
    private boolean isInterstitialNormalLoaded = false;
    private boolean isShowInterstitialNormal = false;

    public void loadSplashInterstitialAdsHighFloor(final Context context, String idAdsHighFloor, String idAdsNormal, long timeOut, long timeDelay, boolean showSplashIfReady, NarayanAdCallback adListener) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                //load Ads Splash High floor
                isShowInterstitialNormal = false;
                isInterstitialNormalLoaded = false;
                Admob.getInstance().loadSplashInterstitialAdsHighFloor(context, idAdsHighFloor, timeOut, timeDelay, false, new AdCallback() {

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        if (isInterstitialNormalLoaded) {
                            if (showSplashIfReady) {
                                Admob.getInstance().onShowSplash((AppCompatActivity) context, new AdCallback() {
                                    @Override
                                    public void onNextAction() {
                                        super.onNextAction();
                                        adListener.onNextAction();
                                    }

                                    @Override
                                    public void onAdClosed() {
                                        super.onAdClosed();
                                        adListener.onAdClosed();
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        super.onAdClicked();
                                        adListener.onAdClicked();
                                    }

                                    @Override
                                    public void onAdFailedToShow(@Nullable AdError adError) {
                                        super.onAdFailedToShow(adError);
                                        adListener.onAdFailedToShow(new NarayanAdError(adError));
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        super.onAdImpression();
                                        adListener.onAdImpression();
                                    }
                                });
                            } else {
                                adListener.onInterstitialLoad(new NarayanInterstitialAd(Admob.getInstance().getmInterstitialSplash()));
                            }
                        } else {
                            //waiting for ad interstitial normal loaded
                            isShowInterstitialNormal = true;
                        }
                    }

                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        if (showSplashIfReady) {
                            Admob.getInstance().onShowSplashHighFloor((AppCompatActivity) context, new AdCallback() {
                                @Override
                                public void onNextAction() {
                                    super.onNextAction();
                                    adListener.onNextAction();
                                }

                                @Override
                                public void onAdClosed() {
                                    super.onAdClosed();
                                    adListener.onAdClosed();
                                }

                                @Override
                                public void onAdClicked() {
                                    super.onAdClicked();
                                    adListener.onAdClicked();
                                }

                                @Override
                                public void onAdFailedToShow(@Nullable AdError adError) {
                                    super.onAdFailedToShow(adError);
                                    adListener.onAdFailedToShow(new NarayanAdError(adError));
                                }

                                @Override
                                public void onAdImpression() {
                                    super.onAdImpression();
                                    adListener.onAdImpression();
                                }
                            });
                        } else {
                            adListener.onAdSplashReady();
                        }
                    }

                });

                //load Ads Splash Normal
                Admob.getInstance().loadSplashInterstitialAds(context, idAdsNormal, timeOut, timeDelay, false, new AdCallback() {

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        isInterstitialNormalLoaded = true;
                        if (isShowInterstitialNormal) {
                            adListener.onNextAction();
                        }
                    }

                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        isInterstitialNormalLoaded = true;
                        if (isShowInterstitialNormal) {
                            if (showSplashIfReady) {
                                Admob.getInstance().onShowSplash((AppCompatActivity) context, new AdCallback() {
                                    @Override
                                    public void onNextAction() {
                                        super.onNextAction();
                                        adListener.onNextAction();
                                    }

                                    @Override
                                    public void onAdClosed() {
                                        super.onAdClosed();
                                        adListener.onAdClosed();
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        super.onAdClicked();
                                        adListener.onAdClicked();
                                    }

                                    @Override
                                    public void onAdFailedToShow(@Nullable AdError adError) {
                                        super.onAdFailedToShow(adError);
                                        adListener.onAdFailedToShow(new NarayanAdError(adError));
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        super.onAdImpression();
                                        adListener.onAdImpression();
                                    }
                                });
                            } else {
                                adListener.onInterstitialLoad(new NarayanInterstitialAd(Admob.getInstance().getmInterstitialSplash()));
                            }
                        }
                    }
                });
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadSplashInterstitialAds(context, idAdsNormal, timeOut, timeDelay, showSplashIfReady, new AppLovinCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        adListener.onAdClosed();
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable MaxError i) {
                        super.onAdFailedToLoad(i);
                        adListener.onAdFailedToLoad(new NarayanAdError(i));
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable MaxError adError) {
                        super.onAdFailedToShow(adError);
                        adListener.onAdFailedToShow(new NarayanAdError(adError));
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        adListener.onAdLoaded();
                    }

                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        adListener.onAdSplashReady();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (adListener != null) {
                            adListener.onAdClicked();
                        }
                    }
                });
        }
    }

    private boolean isFinishedLoadInter = false;
    private boolean isShowInter = false;

    public void loadAppOpenSplashSameTime(
            final Context context,
            String interId,
            long timeOut,
            long timeDelay,
            boolean showSplashIfReady,
            NarayanAdCallback adListener
    ) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                isFinishedLoadInter = false;
                isShowInter = false;
                Admob.getInstance().loadSplashInterstitialAds(context, interId, timeOut, timeDelay, false, new AdCallback() {
                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        isFinishedLoadInter = true;
                        if (isShowInter) {
                            if (showSplashIfReady) {
                                Admob.getInstance().onShowSplash((AppCompatActivity) context, new AdCallback() {
                                    @Override
                                    public void onAdFailedToShow(@Nullable AdError adError) {
                                        super.onAdFailedToShow(adError);
                                        adListener.onAdFailedToShow(new NarayanAdError(adError));
                                    }

                                    @Override
                                    public void onNextAction() {
                                        super.onNextAction();
                                        adListener.onNextAction();
                                    }

                                    @Override
                                    public void onAdClosed() {
                                        super.onAdClosed();
                                        adListener.onAdClosed();
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        super.onAdImpression();
                                        adListener.onAdImpression();
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        super.onAdClicked();
                                        adListener.onAdClicked();
                                    }
                                });
                            } else {
                                adListener.onInterstitialLoad(new NarayanInterstitialAd(Admob.getInstance().getmInterstitialSplash()));
                            }
                        }
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        isFinishedLoadInter = true;
                        if (isShowInter) {
                            adListener.onNextAction();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        isFinishedLoadInter = true;
                        if (isShowInter) {
                            adListener.onAdFailedToLoad(new NarayanAdError(i));
                        }
                    }
                });
                AppOpenManager.getInstance().loadOpenAppAdSplash(context, timeDelay, timeOut, showSplashIfReady, new AdCallback() {
                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        if (isFinishedLoadInter) {
                            if (Admob.getInstance().getmInterstitialSplash() != null) {
                                if (showSplashIfReady) {
                                    Admob.getInstance().onShowSplash((AppCompatActivity) context, new AdCallback() {
                                        @Override
                                        public void onAdFailedToShow(@Nullable AdError adError) {
                                            super.onAdFailedToShow(adError);
                                            adListener.onAdFailedToShow(new NarayanAdError(adError));
                                        }

                                        @Override
                                        public void onNextAction() {
                                            super.onNextAction();
                                            adListener.onNextAction();
                                        }

                                        @Override
                                        public void onAdClosed() {
                                            super.onAdClosed();
                                            adListener.onAdClosed();
                                        }

                                        @Override
                                        public void onAdImpression() {
                                            super.onAdImpression();
                                            adListener.onAdImpression();
                                        }

                                        @Override
                                        public void onAdClicked() {
                                            super.onAdClicked();
                                            adListener.onAdClicked();
                                        }
                                    });
                                } else {
                                    adListener.onInterstitialLoad(new NarayanInterstitialAd(Admob.getInstance().getmInterstitialSplash()));
                                }
                            } else {
                                adListener.onAdFailedToLoad(new NarayanAdError(i));
                            }
                        } else {
                            isShowInter = true;
                        }
                    }

                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        adListener.onAdSplashReady();
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        adListener.onAdFailedToShow(new NarayanAdError(adError));
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        adListener.onAdClicked();
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        adListener.onAdImpression();
                    }

                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        adListener.onAdClosed();
                    }
                });
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadSplashInterstitialAds(context, interId, timeOut, timeDelay, showSplashIfReady, new AppLovinCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        adListener.onAdClosed();
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable MaxError i) {
                        super.onAdFailedToLoad(i);
                        adListener.onAdFailedToLoad(new NarayanAdError(i));
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable MaxError adError) {
                        super.onAdFailedToShow(adError);
                        adListener.onAdFailedToShow(new NarayanAdError(adError));
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        adListener.onAdLoaded();
                    }

                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        adListener.onAdSplashReady();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (adListener != null) {
                            adListener.onAdClicked();
                        }
                    }
                });
        }
    }

    public void loadAppOpenSplashAlternate(
            final Context context,
            String interId,
            long timeOut,
            long timeDelay,
            boolean showSplashIfReady,
            NarayanAdCallback adListener
    ) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                AppOpenManager.getInstance().loadOpenAppAdSplash(context, timeDelay, timeOut, showSplashIfReady, new AdCallback() {
                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        adListener.onAdSplashReady();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        Admob.getInstance().loadSplashInterstitialAds(context, interId, timeOut, timeDelay, false, new AdCallback() {
                            @Override
                            public void onAdSplashReady() {
                                super.onAdSplashReady();
                                if (showSplashIfReady) {
                                    Admob.getInstance().onShowSplash((AppCompatActivity) context, new AdCallback() {
                                        @Override
                                        public void onAdClosed() {
                                            super.onAdClosed();
                                            adListener.onAdClosed();
                                        }

                                        @Override
                                        public void onAdFailedToShow(@Nullable AdError adError) {
                                            super.onAdFailedToShow(adError);
                                            adListener.onAdFailedToShow(new NarayanAdError(adError));
                                        }

                                        @Override
                                        public void onAdImpression() {
                                            super.onAdImpression();
                                            adListener.onAdImpression();
                                        }

                                        @Override
                                        public void onAdClicked() {
                                            super.onAdClicked();
                                            adListener.onAdClicked();
                                        }

                                        @Override
                                        public void onNextAction() {
                                            super.onNextAction();
                                            adListener.onNextAction();
                                        }
                                    });
                                } else {
                                    adListener.onInterstitialLoad(new NarayanInterstitialAd(Admob.getInstance().getmInterstitialSplash()));
                                }
                            }

                            @Override
                            public void onNextAction() {
                                super.onNextAction();
                                adListener.onAdFailedToLoad(null);
                            }
                        });
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        adListener.onAdClosed();
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        adListener.onAdImpression();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        adListener.onAdClicked();
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        adListener.onAdFailedToShow(new NarayanAdError(adError));
                    }
                });
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadSplashInterstitialAds(context, interId, timeOut, timeDelay, showSplashIfReady, new AppLovinCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        adListener.onAdClosed();
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable MaxError i) {
                        super.onAdFailedToLoad(i);
                        adListener.onAdFailedToLoad(new NarayanAdError(i));
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable MaxError adError) {
                        super.onAdFailedToShow(adError);
                        adListener.onAdFailedToShow(new NarayanAdError(adError));
                        adListener.onNextAction();
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        adListener.onAdLoaded();
                    }

                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        adListener.onAdSplashReady();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (adListener != null) {
                            adListener.onAdClicked();
                        }
                    }
                });
        }
    }


    public void onShowSplash(AppCompatActivity activity, NarayanAdCallback adListener) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().onShowSplash(activity, new AdCallback() {
                            @Override
                            public void onAdFailedToShow(@Nullable AdError adError) {
                                super.onAdFailedToShow(adError);
                                adListener.onAdFailedToShow(new NarayanAdError(adError));
                            }

                            @Override
                            public void onAdClosed() {
                                super.onAdClosed();
                                adListener.onAdClosed();
                            }

                            @Override
                            public void onNextAction() {
                                super.onNextAction();
                                adListener.onNextAction();
                            }


                        }
                );
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().onShowSplash(activity, new AppLovinCallback() {
                    @Override
                    public void onAdFailedToShow(@Nullable MaxError adError) {
                        super.onAdFailedToShow(adError);
                        adListener.onAdFailedToShow(new NarayanAdError(adError));
                    }

                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        adListener.onAdClosed();
                        adListener.onNextAction();
                    }

                });

        }
    }

    /**
     * Called  on Resume - SplashActivity
     * It call reshow ad splash when ad splash show fail in background
     *
     * @param activity
     * @param callback
     * @param timeDelay time delay before call show ad splash (ms)
     */
    public void onCheckShowSplashWhenFail(AppCompatActivity activity, NarayanAdCallback callback,
                                          int timeDelay) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().onCheckShowSplashWhenFail(activity, new AdCallback() {
                    @Override
                    public void onNextAction() {
                        super.onAdClosed();
                        callback.onNextAction();
                    }


                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        callback.onAdLoaded();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        callback.onAdFailedToLoad(new NarayanAdError(i));
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        callback.onAdFailedToShow(new NarayanAdError(adError));
                    }
                }, timeDelay);
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().onCheckShowSplashWhenFail(activity, new AppLovinCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        callback.onNextAction();
                    }


                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        callback.onAdLoaded();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable MaxError i) {
                        super.onAdFailedToLoad(i);
                        callback.onAdFailedToLoad(new NarayanAdError(i));
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable MaxError adError) {
                        super.onAdFailedToShow(adError);
                        callback.onAdFailedToShow(new NarayanAdError(adError));
                    }
                }, timeDelay);
        }
    }


    public void onCheckShowSplashHighFloorWhenFail(AppCompatActivity activity, NarayanAdCallback callback,
                                                   int timeDelay) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().onCheckShowSplashHighFloorWhenFail(activity, new AdCallback() {
                    @Override
                    public void onNextAction() {
                        super.onAdClosed();
                        callback.onNextAction();
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        callback.onAdLoaded();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        callback.onAdFailedToLoad(new NarayanAdError(i));
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        callback.onAdFailedToShow(new NarayanAdError(adError));
                    }
                }, timeDelay);
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().onCheckShowSplashWhenFail(activity, new AppLovinCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        callback.onNextAction();
                    }


                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        callback.onAdLoaded();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable MaxError i) {
                        super.onAdFailedToLoad(i);
                        callback.onAdFailedToLoad(new NarayanAdError(i));
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable MaxError adError) {
                        super.onAdFailedToShow(adError);
                        callback.onAdFailedToShow(new NarayanAdError(adError));
                    }
                }, timeDelay);
        }
    }

    /**
     * Result a ApInterstitialAd in onInterstitialLoad
     *
     * @param context
     * @param id         admob or max mediation
     * @param adListener
     */
    public NarayanInterstitialAd getInterstitialAds(Context context, String id, NarayanAdCallback adListener) {
        NarayanInterstitialAd apInterstitialAd = new NarayanInterstitialAd();
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().getInterstitialAds(context, id, new AdCallback() {
                    @Override
                    public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                        super.onInterstitialLoad(interstitialAd);
                        Log.e(TAG, "Admob onInterstitialLoad");
                        apInterstitialAd.setInterstitialAd(interstitialAd);
                        adListener.onInterstitialLoad(apInterstitialAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        adListener.onAdFailedToLoad(new NarayanAdError(i));
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        adListener.onAdFailedToShow(new NarayanAdError(adError));
                    }

                });
                return apInterstitialAd;

            case NarayanAdConfig.PROVIDER_MAX:
                MaxInterstitialAd maxInterstitialAd = AppLovin.getInstance().getInterstitialAds(context, id);
                maxInterstitialAd.setListener(new MaxAdListener() {

                    @Override
                    public void onAdLoaded(MaxAd ad) {
                        Log.e(TAG, "Max onInterstitialLoad: ");
                        apInterstitialAd.setMaxInterstitialAd(maxInterstitialAd);
                        adListener.onInterstitialLoad(apInterstitialAd);
                    }

                    @Override
                    public void onAdDisplayed(MaxAd ad) {

                    }

                    @Override
                    public void onAdHidden(MaxAd ad) {
                        adListener.onAdClosed();
                    }

                    @Override
                    public void onAdClicked(MaxAd ad) {
                        adListener.onAdClicked();
                    }

                    @Override
                    public void onAdLoadFailed(String adUnitId, MaxError error) {
                        adListener.onAdFailedToLoad(new NarayanAdError(error));
                    }

                    @Override
                    public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                        adListener.onAdFailedToShow(new NarayanAdError(error));
                    }
                });
                apInterstitialAd.setMaxInterstitialAd(maxInterstitialAd);
                return apInterstitialAd;
            default:
                return apInterstitialAd;
        }
    }

    /**
     * Result a ApInterstitialAd in onInterstitialLoad
     *
     * @param context
     * @param id      admob or max mediation
     */
    public NarayanInterstitialAd getInterstitialAds(Context context, String id) {
        NarayanInterstitialAd apInterstitialAd = new NarayanInterstitialAd();
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().getInterstitialAds(context, id, new AdCallback() {
                    @Override
                    public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                        super.onInterstitialLoad(interstitialAd);
                        Log.e(TAG, "Admob onInterstitialLoad: ");
                        apInterstitialAd.setInterstitialAd(interstitialAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                    }

                });
                return apInterstitialAd;

            case NarayanAdConfig.PROVIDER_MAX:
                MaxInterstitialAd maxInterstitialAd = AppLovin.getInstance().getInterstitialAds(context, id);
                maxInterstitialAd.setListener(new MaxAdListener() {

                    @Override
                    public void onAdLoaded(MaxAd ad) {
                        Log.e(TAG, "Max onInterstitialLoad: ");
                        apInterstitialAd.setMaxInterstitialAd(maxInterstitialAd);
                    }

                    @Override
                    public void onAdDisplayed(MaxAd ad) {

                    }

                    @Override
                    public void onAdHidden(MaxAd ad) {
                    }

                    @Override
                    public void onAdClicked(MaxAd ad) {
                    }

                    @Override
                    public void onAdLoadFailed(String adUnitId, MaxError error) {
                    }

                    @Override
                    public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                    }
                });
                apInterstitialAd.setMaxInterstitialAd(maxInterstitialAd);
                return apInterstitialAd;
            default:
                return apInterstitialAd;
        }
    }

    /**
     * Called force show ApInterstitialAd when ready
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     */
    public void forceShowInterstitial(Context context, NarayanInterstitialAd mInterstitialAd,
                                      final NarayanAdCallback callback) {
        forceShowInterstitial(context, mInterstitialAd, callback, false);
    }

    /**
     * Called force show ApInterstitialAd when ready
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     * @param shouldReloadAds auto reload ad when ad close
     */
    public void forceShowInterstitial(@NonNull Context context, NarayanInterstitialAd mInterstitialAd,
                                      @NonNull final NarayanAdCallback callback, boolean shouldReloadAds) {
        if (System.currentTimeMillis() - SharePreferenceUtils.getLastImpressionInterstitialTime(context)
                < NarayanAd.getInstance().adConfig.getIntervalInterstitialAd() * 1000L
        ) {
            Log.i(TAG, "forceShowInterstitial: ignore by interval impression interstitial time");
            callback.onNextAction();
            return;
        }
        if (mInterstitialAd == null || mInterstitialAd.isNotReady()) {
            Log.e(TAG, "forceShowInterstitial: ApInterstitialAd is not ready");
            callback.onNextAction();
            return;
        }
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                AdCallback adCallback = new AdCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        Log.e(TAG, "onAdClosed: ");
                        callback.onAdClosed();
                        if (shouldReloadAds) {
                            Admob.getInstance().getInterstitialAds(context, mInterstitialAd.getInterstitialAd().getAdUnitId(), new AdCallback() {
                                @Override
                                public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                                    super.onInterstitialLoad(interstitialAd);
                                    Log.e(TAG, "Admob shouldReloadAds success");
                                    mInterstitialAd.setInterstitialAd(interstitialAd);
                                    callback.onInterstitialLoad(mInterstitialAd);
                                }

                                @Override
                                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                                    super.onAdFailedToLoad(i);
                                    mInterstitialAd.setInterstitialAd(null);
                                    callback.onAdFailedToLoad(new NarayanAdError(i));
                                }

                                @Override
                                public void onAdFailedToShow(@Nullable AdError adError) {
                                    super.onAdFailedToShow(adError);
                                    callback.onAdFailedToShow(new NarayanAdError(adError));
                                }

                            });
                        } else {
                            mInterstitialAd.setInterstitialAd(null);
                        }
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        Log.e(TAG, "onNextAction: ");
                        callback.onNextAction();
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        Log.e(TAG, "onAdFailedToShow: ");
                        callback.onAdFailedToShow(new NarayanAdError(adError));
                        if (shouldReloadAds) {
                            Admob.getInstance().getInterstitialAds(context, mInterstitialAd.getInterstitialAd().getAdUnitId(), new AdCallback() {
                                @Override
                                public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                                    super.onInterstitialLoad(interstitialAd);
                                    Log.e(TAG, "Admob shouldReloadAds success");
                                    mInterstitialAd.setInterstitialAd(interstitialAd);
                                    callback.onInterstitialLoad(mInterstitialAd);
                                }

                                @Override
                                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                                    super.onAdFailedToLoad(i);
                                    callback.onAdFailedToLoad(new NarayanAdError(i));
                                }

                                @Override
                                public void onAdFailedToShow(@Nullable AdError adError) {
                                    super.onAdFailedToShow(adError);
                                    callback.onAdFailedToShow(new NarayanAdError(adError));
                                }

                            });
                        } else {
                            mInterstitialAd.setInterstitialAd(null);
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        callback.onAdClicked();
                    }

                    @Override
                    public void onInterstitialShow() {
                        super.onInterstitialShow();
                        callback.onInterstitialShow();
                    }
                };
                Admob.getInstance().forceShowInterstitial(context, mInterstitialAd.getInterstitialAd(), adCallback);
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().forceShowInterstitial(context, mInterstitialAd.getMaxInterstitialAd(), new AdCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        callback.onAdClosed();
                        callback.onNextAction();
                        if (shouldReloadAds)
                            mInterstitialAd.getMaxInterstitialAd().loadAd();

                    }

                    @Override
                    public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                        super.onInterstitialLoad(interstitialAd);
                        Log.e(TAG, "Max inter onAdLoaded:");
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        callback.onAdFailedToShow(new NarayanAdError(adError));
                        if (shouldReloadAds)
                            mInterstitialAd.getMaxInterstitialAd().loadAd();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        callback.onAdClicked();
                    }

                    @Override
                    public void onInterstitialShow() {
                        super.onInterstitialShow();
                        callback.onInterstitialShow();
                    }
                }, false);
        }
    }

    /**
     * Called force show ApInterstitialAd when reach the number of clicks show ads
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     * @param shouldReloadAds auto reload ad when ad close
     */
    public void showInterstitialAdByTimes(Context context, NarayanInterstitialAd mInterstitialAd,
                                          final NarayanAdCallback callback, boolean shouldReloadAds) {
        if (mInterstitialAd.isNotReady()) {
            Log.e(TAG, "forceShowInterstitial: ApInterstitialAd is not ready");
            callback.onAdFailedToShow(new NarayanAdError("ApInterstitialAd is not ready"));
            return;
        }
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                AdCallback adCallback = new AdCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        Log.e(TAG, "onAdClosed: ");
                        callback.onAdClosed();
                        if (shouldReloadAds) {
                            Admob.getInstance().getInterstitialAds(context, mInterstitialAd.getInterstitialAd().getAdUnitId(), new AdCallback() {
                                @Override
                                public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                                    super.onInterstitialLoad(interstitialAd);
                                    Log.e(TAG, "Admob shouldReloadAds success");
                                    mInterstitialAd.setInterstitialAd(interstitialAd);
                                    callback.onInterstitialLoad(mInterstitialAd);
                                }

                                @Override
                                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                                    super.onAdFailedToLoad(i);
                                    mInterstitialAd.setInterstitialAd(null);
                                    callback.onAdFailedToLoad(new NarayanAdError(i));
                                }

                                @Override
                                public void onAdFailedToShow(@Nullable AdError adError) {
                                    super.onAdFailedToShow(adError);
                                    callback.onAdFailedToShow(new NarayanAdError(adError));
                                }

                            });
                        } else {
                            mInterstitialAd.setInterstitialAd(null);
                        }
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        Log.e(TAG, "onNextAction: ");
                        callback.onNextAction();
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        Log.e(TAG, "onAdFailedToShow: ");
                        callback.onAdFailedToShow(new NarayanAdError(adError));
                        if (shouldReloadAds) {
                            Admob.getInstance().getInterstitialAds(context, mInterstitialAd.getInterstitialAd().getAdUnitId(), new AdCallback() {
                                @Override
                                public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                                    super.onInterstitialLoad(interstitialAd);
                                    Log.e(TAG, "Admob shouldReloadAds success");
                                    mInterstitialAd.setInterstitialAd(interstitialAd);
                                    callback.onInterstitialLoad(mInterstitialAd);
                                }

                                @Override
                                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                                    super.onAdFailedToLoad(i);
                                    callback.onAdFailedToLoad(new NarayanAdError(i));
                                }

                                @Override
                                public void onAdFailedToShow(@Nullable AdError adError) {
                                    super.onAdFailedToShow(adError);
                                    callback.onAdFailedToShow(new NarayanAdError(adError));
                                }

                            });
                        } else {
                            mInterstitialAd.setInterstitialAd(null);
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (callback != null) {
                            callback.onAdClicked();
                        }
                    }

                    @Override
                    public void onInterstitialShow() {
                        super.onInterstitialShow();
                        if (callback != null) {
                            callback.onInterstitialShow();
                        }
                    }
                };
                Admob.getInstance().showInterstitialAdByTimes(context, mInterstitialAd.getInterstitialAd(), adCallback);
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().showInterstitialAdByTimes(context, mInterstitialAd.getMaxInterstitialAd(), new AdCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        callback.onAdClosed();
                        callback.onNextAction();
                        if (shouldReloadAds)
                            mInterstitialAd.getMaxInterstitialAd().loadAd();

                    }

                    @Override
                    public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                        super.onInterstitialLoad(interstitialAd);
                        Log.e(TAG, "Max inter onAdLoaded:");
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        callback.onAdFailedToShow(new NarayanAdError(adError));
                        if (shouldReloadAds)
                            mInterstitialAd.getMaxInterstitialAd().loadAd();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (callback != null) {
                            callback.onAdClicked();
                        }
                    }

                    @Override
                    public void onInterstitialShow() {
                        super.onInterstitialShow();
                        if (callback != null) {
                            callback.onInterstitialShow();
                        }
                    }
                }, false);
        }
    }

    /**
     * Load native ad and auto populate ad to view in activity
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     */
    public void loadNativeAd(final Activity activity, String id,
                             int layoutCustomNative) {
        FrameLayout adPlaceHolder = activity.findViewById(R.id.fl_adplaceholder);
        ShimmerFrameLayout containerShimmerLoading = activity.findViewById(R.id.shimmer_container_native);

        if (AppPurchase.getInstance().isPurchased()) {
            if (containerShimmerLoading != null) {
                containerShimmerLoading.stopShimmer();
                containerShimmerLoading.setVisibility(View.GONE);
            }
            return;
        }
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().loadNativeAd(((Context) activity), id, new AdCallback() {
                    @Override
                    public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                        super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                        populateNativeAdView(activity, new NarayanNativeAd(layoutCustomNative, unifiedNativeAd), adPlaceHolder, containerShimmerLoading);
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        Log.e(TAG, "onAdFailedToLoad : NativeAd");
                    }
                });
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadNativeAd(activity, id, layoutCustomNative, new AppLovinCallback() {
                    @Override
                    public void onUnifiedNativeAdLoaded(MaxNativeAdView unifiedNativeAd) {
                        super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                        populateNativeAdView(activity, new NarayanNativeAd(layoutCustomNative, unifiedNativeAd), adPlaceHolder, containerShimmerLoading);
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable MaxError i) {
                        super.onAdFailedToLoad(i);
                        Log.e(TAG, "onAdFailedToLoad : NativeAd");
                    }
                });
                break;
        }
    }

    /**
     * Load native ad and auto populate ad to adPlaceHolder and hide containerShimmerLoading
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     * @param adPlaceHolder
     * @param containerShimmerLoading
     */
    public void loadNativeAd(final Activity activity, String id,
                             int layoutCustomNative, FrameLayout adPlaceHolder, ShimmerFrameLayout
                                     containerShimmerLoading) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().loadNativeAd(((Context) activity), id, new AdCallback() {
                    @Override
                    public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                        super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                        populateNativeAdView(activity, new NarayanNativeAd(layoutCustomNative, unifiedNativeAd), adPlaceHolder, containerShimmerLoading);
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        Log.e(TAG, "onAdFailedToLoad : NativeAd");
                    }
                });
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadNativeAd(activity, id, layoutCustomNative, new AppLovinCallback() {
                    @Override
                    public void onUnifiedNativeAdLoaded(MaxNativeAdView unifiedNativeAd) {
                        super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                        populateNativeAdView(activity, new NarayanNativeAd(layoutCustomNative, unifiedNativeAd), adPlaceHolder, containerShimmerLoading);
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable MaxError i) {
                        super.onAdFailedToLoad(i);
                        Log.e(TAG, "onAdFailedToLoad : NativeAd");
                    }
                });
                break;
        }
    }

    /**
     * Load native ad and auto populate ad to adPlaceHolder and hide containerShimmerLoading
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     * @param adPlaceHolder
     * @param containerShimmerLoading
     */
    public void loadNativeAd(final Activity activity, String id,
                             int layoutCustomNative, FrameLayout adPlaceHolder, ShimmerFrameLayout
                                     containerShimmerLoading, NarayanAdCallback callback) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().loadNativeAd(((Context) activity), id, new AdCallback() {
                    @Override
                    public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                        super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                        callback.onNativeAdLoaded(new NarayanNativeAd(layoutCustomNative, unifiedNativeAd));
                        populateNativeAdView(activity, new NarayanNativeAd(layoutCustomNative, unifiedNativeAd), adPlaceHolder, containerShimmerLoading);
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        callback.onAdImpression();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        callback.onAdFailedToLoad(new NarayanAdError(i));
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        callback.onAdFailedToShow(new NarayanAdError(adError));
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        callback.onAdClicked();
                    }
                });
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadNativeAd(activity, id, layoutCustomNative, new AppLovinCallback() {
                    @Override
                    public void onUnifiedNativeAdLoaded(MaxNativeAdView unifiedNativeAd) {
                        super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                        callback.onNativeAdLoaded(new NarayanNativeAd(layoutCustomNative, unifiedNativeAd));
                        populateNativeAdView(activity, new NarayanNativeAd(layoutCustomNative, unifiedNativeAd), adPlaceHolder, containerShimmerLoading);
                        callback.onAdImpression();
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable MaxError i) {
                        super.onAdFailedToLoad(i);
                        callback.onAdFailedToLoad(new NarayanAdError(i));
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        callback.onAdClicked();
                    }
                });
                break;
        }
    }

    /**
     * Result a ApNativeAd in onUnifiedNativeAdLoaded when native ad loaded
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     * @param callback
     */
    public void loadNativeAdResultCallback(final Activity activity, String id,
                                           int layoutCustomNative, NarayanAdCallback callback) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().loadNativeAd(((Context) activity), id, new AdCallback() {
                    @Override
                    public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                        super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                        callback.onNativeAdLoaded(new NarayanNativeAd(layoutCustomNative, unifiedNativeAd));
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        callback.onAdFailedToLoad(new NarayanAdError(i));
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        callback.onAdFailedToShow(new NarayanAdError(adError));
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        callback.onAdClicked();
                    }
                });
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().loadNativeAd(activity, id, layoutCustomNative, new AppLovinCallback() {
                    @Override
                    public void onUnifiedNativeAdLoaded(MaxNativeAdView unifiedNativeAd) {
                        super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                        callback.onNativeAdLoaded(new NarayanNativeAd(layoutCustomNative, unifiedNativeAd));
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable MaxError i) {
                        super.onAdFailedToLoad(i);
                        callback.onAdFailedToLoad(new NarayanAdError(i));
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        callback.onAdClicked();
                    }
                });
                break;
        }
    }

    /**
     * Populate Unified Native Ad to View
     *
     * @param activity
     * @param apNativeAd
     * @param adPlaceHolder
     * @param containerShimmerLoading
     */
    public void populateNativeAdView(Activity activity, NarayanNativeAd apNativeAd, FrameLayout
            adPlaceHolder, ShimmerFrameLayout containerShimmerLoading) {
        if (apNativeAd.getAdmobNativeAd() == null && apNativeAd.getNativeView() == null) {
            containerShimmerLoading.setVisibility(View.GONE);
            Log.e(TAG, "populateNativeAdView failed : native is not loaded ");
            return;
        }
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(apNativeAd.getLayoutCustomNative(), null);
                containerShimmerLoading.stopShimmer();
                containerShimmerLoading.setVisibility(View.GONE);
                adPlaceHolder.setVisibility(View.VISIBLE);
                Admob.getInstance().populateUnifiedNativeAdView(apNativeAd.getAdmobNativeAd(), adView);
                adPlaceHolder.removeAllViews();
                adPlaceHolder.addView(adView);
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                containerShimmerLoading.stopShimmer();
                containerShimmerLoading.setVisibility(View.GONE);
                adPlaceHolder.setVisibility(View.VISIBLE);
                adPlaceHolder.removeAllViews();
                if (apNativeAd.getNativeView().getParent() != null) {
                    ((ViewGroup) apNativeAd.getNativeView().getParent()).removeAllViews();
                }
                adPlaceHolder.addView(apNativeAd.getNativeView());
        }
    }


    public NarayanRewardAd getRewardAd(Activity activity, String id) {
        NarayanRewardAd apRewardAd = new NarayanRewardAd();
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().initRewardAds(activity, id, new AdCallback() {

                    @Override
                    public void onRewardAdLoaded(RewardedAd rewardedAd) {
                        super.onRewardAdLoaded(rewardedAd);
                        Log.i(TAG, "getRewardAd AdLoaded: ");
                        apRewardAd.setAdmobReward(rewardedAd);
                    }
                });
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                MaxRewardedAd maxRewardedAd = AppLovin.getInstance().getRewardAd(activity, id, new AppLovinCallback() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                    }
                });
                apRewardAd.setMaxReward(maxRewardedAd);
        }
        return apRewardAd;
    }

    public NarayanRewardAd getRewardAdInterstitial(Activity activity, String id) {
        NarayanRewardAd apRewardAd = new NarayanRewardAd();
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().getRewardInterstitial(activity, id, new AdCallback() {

                    @Override
                    public void onRewardAdLoaded(RewardedInterstitialAd rewardedAd) {
                        super.onRewardAdLoaded(rewardedAd);
                        Log.i(TAG, "getRewardAdInterstitial AdLoaded: ");
                        apRewardAd.setAdmobReward(rewardedAd);
                    }
                });
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                MaxRewardedAd maxRewardedAd = AppLovin.getInstance().getRewardAd(activity, id, new AppLovinCallback() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                    }
                });
                apRewardAd.setMaxReward(maxRewardedAd);
        }
        return apRewardAd;
    }

    public NarayanRewardAd getRewardAd(Activity activity, String id, NarayanAdCallback callback) {
        NarayanRewardAd apRewardAd = new NarayanRewardAd();
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().initRewardAds(activity, id, new AdCallback() {
                    @Override
                    public void onRewardAdLoaded(RewardedAd rewardedAd) {
                        super.onRewardAdLoaded(rewardedAd);
                        apRewardAd.setAdmobReward(rewardedAd);
                        callback.onAdLoaded();
                    }
                });
                return apRewardAd;
            case NarayanAdConfig.PROVIDER_MAX:
                MaxRewardedAd maxRewardedAd = AppLovin.getInstance().getRewardAd(activity, id, new AppLovinCallback() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        callback.onAdLoaded();
                    }
                });
                apRewardAd.setMaxReward(maxRewardedAd);
                return apRewardAd;
        }
        return apRewardAd;
    }

    public NarayanRewardAd getRewardInterstitialAd(Activity activity, String id, NarayanAdCallback callback) {
        NarayanRewardAd apRewardAd = new NarayanRewardAd();
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                Admob.getInstance().getRewardInterstitial(activity, id, new AdCallback() {
                    @Override
                    public void onRewardAdLoaded(RewardedInterstitialAd rewardedAd) {
                        super.onRewardAdLoaded(rewardedAd);
                        apRewardAd.setAdmobReward(rewardedAd);
                        callback.onAdLoaded();
                    }
                });
                return apRewardAd;
            case NarayanAdConfig.PROVIDER_MAX:
                MaxRewardedAd maxRewardedAd = AppLovin.getInstance().getRewardAd(activity, id, new AppLovinCallback() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        callback.onAdLoaded();
                    }
                });
                apRewardAd.setMaxReward(maxRewardedAd);
                return apRewardAd;
        }
        return apRewardAd;
    }

    public void forceShowRewardAd(Activity activity, NarayanRewardAd apRewardAd, NarayanAdCallback
            callback) {
        if (!apRewardAd.isReady()) {
            Log.e(TAG, "forceShowRewardAd fail: reward ad not ready");
            callback.onNextAction();
            return;
        }
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_ADMOB:
                if (apRewardAd.isRewardInterstitial()) {
                    Admob.getInstance().showRewardInterstitial(activity, apRewardAd.getAdmobRewardInter(), new RewardCallback() {

                        @Override
                        public void onUserEarnedReward(RewardItem var1) {
                            callback.onUserEarnedReward(new NarayanRewardItem(var1));
                        }

                        @Override
                        public void onRewardedAdClosed() {
                            apRewardAd.clean();
                            callback.onNextAction();
                        }

                        @Override
                        public void onRewardedAdFailedToShow(int codeError) {
                            apRewardAd.clean();
                            callback.onAdFailedToShow(new NarayanAdError(new AdError(codeError, "note msg", "Reward")));
                        }

                        @Override
                        public void onAdClicked() {
                            if (callback != null) {
                                callback.onAdClicked();
                            }
                        }
                    });
                } else {
                    Admob.getInstance().showRewardAds(activity, apRewardAd.getAdmobReward(), new RewardCallback() {

                        @Override
                        public void onUserEarnedReward(RewardItem var1) {
                            callback.onUserEarnedReward(new NarayanRewardItem(var1));
                        }

                        @Override
                        public void onRewardedAdClosed() {
                            apRewardAd.clean();
                            callback.onNextAction();
                        }

                        @Override
                        public void onRewardedAdFailedToShow(int codeError) {
                            apRewardAd.clean();
                            callback.onAdFailedToShow(new NarayanAdError(new AdError(codeError, "note msg", "Reward")));
                        }

                        @Override
                        public void onAdClicked() {
                            if (callback != null) {
                                callback.onAdClicked();
                            }
                        }
                    });
                }
                break;
            case NarayanAdConfig.PROVIDER_MAX:
                AppLovin.getInstance().showRewardAd(activity, apRewardAd.getMaxReward(), new AppLovinCallback() {
                    @Override
                    public void onUserRewarded(MaxReward reward) {
                        super.onUserRewarded(reward);
                        callback.onUserEarnedReward(new NarayanRewardItem(reward));
                    }

                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        apRewardAd.clean();
                        callback.onNextAction();
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable MaxError adError) {
                        super.onAdFailedToShow(adError);
                        apRewardAd.clean();
                        callback.onAdFailedToShow(new NarayanAdError(adError));
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (callback != null) {
                            callback.onAdClicked();
                        }
                    }
                });
        }
    }

    /**
     * Result a NarayanAdAdapter with ad native repeating interval
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     * @param layoutAdPlaceHolder
     * @param originalAdapter
     * @param listener
     * @param repeatingInterval
     * @return
     */
    public NarayanAdAdapter getNativeRepeatAdapter(Activity activity, String id, int layoutCustomNative, int layoutAdPlaceHolder, RecyclerView.Adapter originalAdapter,
                                                   NarayanAdPlacer.Listener listener, int repeatingInterval) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_MAX:
                MaxAdPlacer.Listener maxListener = new MaxAdPlacer.Listener() {
                    @Override
                    public void onAdLoaded(int i) {
                        listener.onAdLoaded(i);
                        listener.onAdImpression();
                    }

                    @Override
                    public void onAdRemoved(int i) {
                        listener.onAdRemoved(i);
                    }

                    @Override
                    public void onAdClicked(MaxAd maxAd) {
                        NarayanLogEventManager.logClickAdsEvent(activity, maxAd.getAdUnitId());
                        listener.onAdClicked();
                    }

                    @Override
                    public void onAdRevenuePaid(MaxAd maxAd) {
                        listener.onAdRevenuePaid(new NarayanAdValue(maxAd));
                    }
                };
                MaxRecyclerAdapter adAdapter = AppLovin.getInstance().getNativeRepeatAdapter(activity, id, layoutCustomNative,
                        originalAdapter, maxListener, repeatingInterval);

                return new NarayanAdAdapter(adAdapter);
            default:
                return new NarayanAdAdapter(Admob.getInstance().getNativeRepeatAdapter(activity, id, layoutCustomNative, layoutAdPlaceHolder,
                        originalAdapter, listener, repeatingInterval));
        }

    }

    /**
     * Result a NarayanAdAdapter with ad native fixed in position
     *
     * @param activity
     * @param id
     * @param layoutCustomNative
     * @param layoutAdPlaceHolder
     * @param originalAdapter
     * @param listener
     * @param position
     * @return
     */
    public NarayanAdAdapter getNativeFixedPositionAdapter(Activity activity, String id, int layoutCustomNative, int layoutAdPlaceHolder, RecyclerView.Adapter originalAdapter,
                                                          NarayanAdPlacer.Listener listener, int position) {
        switch (adConfig.getMediationProvider()) {
            case NarayanAdConfig.PROVIDER_MAX:
                MaxAdPlacer.Listener maxListener = new MaxAdPlacer.Listener() {
                    @Override
                    public void onAdLoaded(int i) {
                        listener.onAdLoaded(i);
                        listener.onAdImpression();
                    }

                    @Override
                    public void onAdRemoved(int i) {
                        listener.onAdRemoved(i);
                    }

                    @Override
                    public void onAdClicked(MaxAd maxAd) {
                        NarayanLogEventManager.logClickAdsEvent(activity, maxAd.getAdUnitId());
                        listener.onAdClicked();
                    }

                    @Override
                    public void onAdRevenuePaid(MaxAd maxAd) {
                        listener.onAdRevenuePaid(new NarayanAdValue(maxAd));
                    }
                };
                MaxRecyclerAdapter adAdapter = AppLovin.getInstance().getNativeFixedPositionAdapter(activity, id, layoutCustomNative,
                        originalAdapter, maxListener, position);
                adAdapter.loadAds();
                return new NarayanAdAdapter(adAdapter);
            default:
                return new NarayanAdAdapter(Admob.getInstance().getNativeFixedPositionAdapter(activity, id, layoutCustomNative, layoutAdPlaceHolder,
                        originalAdapter, listener, position));
        }
    }
}
