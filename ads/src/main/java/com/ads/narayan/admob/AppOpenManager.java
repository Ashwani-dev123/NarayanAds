package com.ads.narayan.admob;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.ads.narayan.R;
import com.ads.narayan.billing.AppPurchase;
import com.ads.narayan.dialog.PrepareLoadingAdsDialog;
import com.ads.narayan.dialog.ResumeLoadingDialog;
import com.ads.narayan.event.NarayanLogEventManager;
import com.ads.narayan.funtion.AdCallback;
import com.ads.narayan.funtion.AdType;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AppOpenManager implements Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private static final String TAG = "AppOpenManager";
    public static final String AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/3419835294";

    private static volatile AppOpenManager INSTANCE;
    private AppOpenAd appResumeAd = null;
    private AppOpenAd splashAd = null;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;
    private FullScreenContentCallback fullScreenContentCallback;

    private String appResumeAdId;

    public void setSplashAdId(String splashAdId) {
        this.splashAdId = splashAdId;
    }

    private String splashAdId;

    private Activity currentActivity;

    private Application myApplication;

    private static boolean isShowingAd = false;
    private long appResumeLoadTime = 0;
    private long splashLoadTime = 0;
    private int splashTimeout = 0;

    private boolean isInitialized = false;// on  - off ad resume on app
    private boolean isAppResumeEnabled = true;
    private boolean isInterstitialShowing = false;
    private boolean enableScreenContentCallback = false; // default =  true when use splash & false after show splash
    private boolean disableAdResumeByClickAction = false;
    private final List<Class> disabledAppOpenList;
    private Class splashActivity;

    private boolean isTimeout = false;
    private static final int TIMEOUT_MSG = 11;

    private Handler timeoutHandler;


    private AppOpenManager() {
        disabledAppOpenList = new ArrayList<>();
    }

    public static synchronized AppOpenManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AppOpenManager();
        }
        return INSTANCE;
    }


    public void init(Application application, String appOpenAdId) {
        isInitialized = true;
        disableAdResumeByClickAction = false;
        this.myApplication = application;
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        this.appResumeAdId = appOpenAdId;

    }

    public boolean isInitialized() {
        return isInitialized;
    }


    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    public void setEnableScreenContentCallback(boolean enableScreenContentCallback) {
        this.enableScreenContentCallback = enableScreenContentCallback;
    }

    public boolean isInterstitialShowing() {
        return isInterstitialShowing;
    }

    public void setInterstitialShowing(boolean interstitialShowing) {
        isInterstitialShowing = interstitialShowing;
    }


    public void disableAdResumeByClickAction(){
        disableAdResumeByClickAction = true;
    }

    public void setDisableAdResumeByClickAction(boolean disableAdResumeByClickAction) {
        this.disableAdResumeByClickAction = disableAdResumeByClickAction;
    }


    public boolean isShowingAd() {
        return isShowingAd;
    }


    public void disableAppResumeWithActivity(Class activityClass) {
        Log.e(TAG, "disableAppResumeWithActivity: " + activityClass.getName());
        disabledAppOpenList.add(activityClass);
    }

    public void enableAppResumeWithActivity(Class activityClass) {
        Log.e(TAG, "enableAppResumeWithActivity: " + activityClass.getName());
        disabledAppOpenList.remove(activityClass);
    }

    public void disableAppResume() {
        isAppResumeEnabled = false;
    }

    public void enableAppResume() {
        isAppResumeEnabled = true;
    }

    public void setSplashActivity(Class splashActivity, String adId, int timeoutInMillis) {
        this.splashActivity = splashActivity;
        splashAdId = adId;
        this.splashTimeout = timeoutInMillis;
    }

    public void setAppResumeAdId(String appResumeAdId) {
        this.appResumeAdId = appResumeAdId;
    }

    public void setFullScreenContentCallback(FullScreenContentCallback callback) {
        this.fullScreenContentCallback = callback;
    }

    public void removeFullScreenContentCallback() {
        this.fullScreenContentCallback = null;
    }


    public void fetchAd(final boolean isSplash) {
        Log.e(TAG, "fetchAd: isSplash = " + isSplash);
        if (isAdAvailable(isSplash)) {
            return;
        }

        loadCallback =
                new AppOpenAd.AppOpenAdLoadCallback() {


                    @Override
                    public void onAdLoaded(AppOpenAd ad) {
                        Log.e(TAG, "onAppOpenAdLoaded: isSplash = " + isSplash);
                        if (!isSplash) {
                            AppOpenManager.this.appResumeAd = ad;
                            AppOpenManager.this.appResumeAd.setOnPaidEventListener(adValue -> {
                                NarayanLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                        adValue,
                                        ad.getAdUnitId(),
                                        ad.getResponseInfo()
                                                .getMediationAdapterClassName(),AdType.APP_OPEN);
                            });
                            AppOpenManager.this.appResumeLoadTime = (new Date()).getTime();
                        } else {
                            AppOpenManager.this.splashAd = ad;
                            AppOpenManager.this.splashAd.setOnPaidEventListener(adValue -> {
                                NarayanLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                        adValue,
                                        ad.getAdUnitId(),
                                        ad.getResponseInfo()
                                                .getMediationAdapterClassName(),AdType.APP_OPEN);
                            });
                            AppOpenManager.this.splashLoadTime = (new Date()).getTime();
                        }


                    }



                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "onAppOpenAdFailedToLoad: isSplash" + isSplash + " message " + loadAdError.getMessage());
//                        if (isSplash && fullScreenContentCallback!=null)
//                            fullScreenContentCallback.onAdDismissedFullScreenContent();
                    }


                };
        if (currentActivity != null) {
            if (AppPurchase.getInstance().isPurchased(currentActivity))
                return;
            if (Arrays.asList(currentActivity.getResources().getStringArray(R.array.list_id_test)).contains(isSplash ? splashAdId : appResumeAdId)) {
                showTestIdAlert(currentActivity, isSplash, isSplash ? splashAdId : appResumeAdId);
            }

        }
        AdRequest request = getAdRequest();
        AppOpenAd.load(
                myApplication, isSplash ? splashAdId : appResumeAdId, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
    }

    private void showTestIdAlert(Context context, boolean isSplash, String id) {
        Notification notification = new NotificationCompat.Builder(context, "warning_ads")
                .setContentTitle("Found test ad id")
                .setContentText((isSplash ? "Splash Ads: " : "AppResume Ads: " + id))
                .setSmallIcon(R.drawable.ic_warning)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("warning_ads",
                    "Warning Ads",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(isSplash ? Admob.SPLASH_ADS : Admob.RESUME_ADS, notification);
//        if (!BuildConfig.DEBUG){
//            throw new RuntimeException("Found test ad id on release");
//        }
    }


    private AdRequest getAdRequest() {

        AdRequest.Builder builder = new AdRequest.Builder();
        //AV Language
        Bundle extras = new Bundle();
        extras.putString("npa", "1");
        extras.putString("GoogleConsent", "en"); // Replace "en" with the desired language code
        builder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
        //AV Language

        return builder.build();
    }

    private boolean wasLoadTimeLessThanNHoursAgo(long loadTime, long numHours) {
        long dateDifference = (new Date()).getTime() - loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }


    public boolean isAdAvailable(boolean isSplash) {
        long loadTime = isSplash ? splashLoadTime : appResumeLoadTime;
        boolean wasLoadTimeLessThanNHoursAgo = wasLoadTimeLessThanNHoursAgo(loadTime, 4);
        Log.e(TAG, "isAdAvailable: " + wasLoadTimeLessThanNHoursAgo);
        return (isSplash ? splashAd != null : appResumeAd != null)
                && wasLoadTimeLessThanNHoursAgo;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = activity;
        Log.e(TAG, "onActivityStarted: " + currentActivity);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
        Log.e(TAG, "onActivityResumed: " + currentActivity);
        if (splashActivity == null) {
            if (!activity.getClass().getName().equals(AdActivity.class.getName())) {
                Log.e(TAG, "onActivityResumed 1: with " + activity.getClass().getName());
                fetchAd(false);
            }
        } else {
            if (!activity.getClass().getName().equals(splashActivity.getName()) && !activity.getClass().getName().equals(AdActivity.class.getName())) {
                Log.e(TAG, "onActivityResumed 2: with " + activity.getClass().getName());
                fetchAd(false);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        currentActivity = null;
        Log.e(TAG, "onActivityDestroyed: null" );
    }

    public void showAdIfAvailable(final boolean isSplash) {
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (currentActivity == null || AppPurchase.getInstance().isPurchased(currentActivity)) {
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                fullScreenContentCallback.onAdDismissedFullScreenContent();
            }
            return;
        }

        Log.e(TAG, "showAdIfAvailable: " + ProcessLifecycleOwner.get().getLifecycle().getCurrentState());
        Log.e(TAG, "showAd isSplash: " + isSplash);
        if (!ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            Log.e(TAG, "showAdIfAvailable: return");
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                fullScreenContentCallback.onAdDismissedFullScreenContent();
            }

            return;
        }

        if (!isShowingAd && isAdAvailable(isSplash)) {
            Log.e(TAG, "Will show ad isSplash:" + isSplash);
            if (isSplash) {
                showAdsWithLoading();
            } else {
                showResumeAds();
            }

        } else {
            Log.e(TAG, "Ad is not ready");
            if (!isSplash) {
                fetchAd(false);
            }
        }
    }

    private void showAdsWithLoading() {
        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            Dialog dialog = null;
            try {
                dialog = new PrepareLoadingAdsDialog(currentActivity);
                try {
                    dialog.show();
                } catch (Exception e) {
                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                        fullScreenContentCallback.onAdDismissedFullScreenContent();
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            final Dialog finalDialog = dialog;
            new Handler().postDelayed(() -> {
                if(splashAd != null){
                    splashAd.setFullScreenContentCallback(
                            new FullScreenContentCallback() {
                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    // Set the reference to null so isAdAvailable() returns false.
                                    appResumeAd = null;
                                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                        fullScreenContentCallback.onAdDismissedFullScreenContent();
                                        enableScreenContentCallback = false;
                                    }
                                    isShowingAd = false;
                                    fetchAd(true);
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(AdError adError) {
                                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                        fullScreenContentCallback.onAdFailedToShowFullScreenContent(adError);
                                    }
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                        fullScreenContentCallback.onAdShowedFullScreenContent();
                                    }
                                    isShowingAd = true;
                                    splashAd = null;
                                }


                                @Override
                                public void onAdClicked() {
                                    super.onAdClicked();
                                    if (currentActivity != null) {
                                        NarayanLogEventManager.logClickAdsEvent(currentActivity, splashAdId);
                                        if (fullScreenContentCallback!= null) {
                                            fullScreenContentCallback.onAdClicked();
                                        }
                                    }
                                }
                            });
                    splashAd.show(currentActivity);
                }


            }, 800);
        }
    }

    Dialog dialog = null;

    private void showResumeAds() {
        if (appResumeAd == null || currentActivity == null || AppPurchase.getInstance().isPurchased(currentActivity)) {
            return;
        }
        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {

            try {
                dismissDialogLoading();
                dialog = new ResumeLoadingDialog(currentActivity);
                try {
                    dialog.show();
                } catch (Exception e) {
                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                        fullScreenContentCallback.onAdDismissedFullScreenContent();

                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
//            new Handler().postDelayed(() -> {
                if (appResumeAd != null) {
                    appResumeAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Set the reference to null so isAdAvailable() returns false.
                            appResumeAd = null;
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback.onAdDismissedFullScreenContent();
                            }
                            isShowingAd = false;
                            fetchAd(false);

                            dismissDialogLoading();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.getMessage());
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback.onAdFailedToShowFullScreenContent(adError);
                            }

                            if (currentActivity != null && !currentActivity.isDestroyed() && dialog != null && dialog.isShowing()) {
                                Log.e(TAG, "dismiss dialog loading ad open: ");
                                try {
                                    dialog.dismiss();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            appResumeAd = null;
                            isShowingAd = false;
                            fetchAd(false);
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback.onAdShowedFullScreenContent();
                            }
                            isShowingAd = true;
                            appResumeAd = null;
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            if (currentActivity != null) {
                                NarayanLogEventManager.logClickAdsEvent(currentActivity, appResumeAdId);
                                if (fullScreenContentCallback!= null) {
                                    fullScreenContentCallback.onAdClicked();
                                }
                            }
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            if (currentActivity != null) {
                                if (fullScreenContentCallback!= null) {
                                    fullScreenContentCallback.onAdImpression();
                                }
                            }
                        }
                    });
                    appResumeAd.show(currentActivity);
                }
//            }, 1000);
        }
    }
    public void loadAndShowSplashAds(final String aId) {
        loadAndShowSplashAds(aId, 0);
    }

    public void loadAndShowSplashAds(final String adId, long delay) {
        isTimeout = false;
        enableScreenContentCallback = true;
        if (currentActivity != null && AppPurchase.getInstance().isPurchased(currentActivity)) {
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                (new Handler()).postDelayed(() -> {
                    fullScreenContentCallback.onAdDismissedFullScreenContent();
                }, delay);
            }
            return;
        }

//        if (isAdAvailable(true)) {
//            showAdIfAvailable(true);
//            return;
//        }

        loadCallback =
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                        Log.e(TAG, "onAppOpenAdLoaded: splash");

                        timeoutHandler.removeCallbacks(runnableTimeout);

                        if (isTimeout) {
                            Log.e(TAG, "onAppOpenAdLoaded: splash timeout");
//                            if (fullScreenContentCallback != null) {
//                                fullScreenContentCallback.onAdDismissedFullScreenContent();
//                                enableScreenContentCallback = false;
//                            }
                        } else {
                            AppOpenManager.this.splashAd = appOpenAd;
                            splashLoadTime = new Date().getTime();
                            appOpenAd.setOnPaidEventListener(adValue -> {
                                NarayanLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                        adValue,
                                        appOpenAd.getAdUnitId(),
                                        appOpenAd.getResponseInfo()
                                                .getMediationAdapterClassName(), AdType.APP_OPEN);
                            });

                            (new Handler()).postDelayed(() -> {
                                showAdIfAvailable(true);
                            }, delay);
                        }
                    }


                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "onAppOpenAdFailedToLoad: splash " + loadAdError.getMessage());
                        if (isTimeout) {
                            Log.e(TAG, "onAdFailedToLoad: splash timeout");
                            return;
                        }
                        if (fullScreenContentCallback != null && enableScreenContentCallback) {
                            (new Handler()).postDelayed(() -> {
                                fullScreenContentCallback.onAdDismissedFullScreenContent();
                            }, delay);
                            enableScreenContentCallback = false;
                        }
                    }

                };
        AdRequest request = getAdRequest();
        AppOpenAd.load(
                myApplication, splashAdId, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);

        if (splashTimeout > 0) {
            timeoutHandler = new Handler();
            timeoutHandler.postDelayed(runnableTimeout, splashTimeout);
        }
    }

    public void loadOpenAppAdSplash(
            Context context,
            long timeDelay,
            long timeOut,
            boolean isShowAdIfReady,
            AdCallback adCallback
    ) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            adCallback.onNextAction();
            return;
        }
        long startLoadAd = System.currentTimeMillis();
        Runnable actionTimeOut = () -> {
            Log.e(TAG, "getAdSplash time out");
            adCallback.onNextAction();
            isShowingAd = false;
        };
        Handler handleTimeOut = new Handler();
        handleTimeOut.postDelayed(actionTimeOut, timeOut);
        AdRequest request = getAdRequest();
        AppOpenAd.load(
                context,
                splashAdId,
                request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        adCallback.onAdFailedToLoad(loadAdError);
                        handleTimeOut.removeCallbacks(actionTimeOut);
                    }

                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                        super.onAdLoaded(appOpenAd);
                        handleTimeOut.removeCallbacks(actionTimeOut);
                        splashAd = appOpenAd;
                        splashAd.setOnPaidEventListener(adValue -> {
                            NarayanLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    splashAd.getAdUnitId(),
                                    splashAd.getResponseInfo()
                                            .getMediationAdapterClassName(), AdType.APP_OPEN);
                        });
                        if (isShowAdIfReady) {
                            long delayTimeLeft = System.currentTimeMillis() - startLoadAd;
                            (new Handler()).postDelayed(() -> {
                                showAppOpenSplash(context, adCallback);
                            }, delayTimeLeft >= timeDelay ? 0 : delayTimeLeft);
                        } else {
                            adCallback.onAdSplashReady();
                        }
                    }
                }
        );
    }

    public void showAppOpenSplash(
            Context context,
            AdCallback adCallback
    ) {
        if (splashAd == null) {
            adCallback.onNextAction();
            return;
        }
        dismissDialogLoading();
        try {
            dialog = new PrepareLoadingAdsDialog(context);
            try {
                dialog.setCancelable(false);
                dialog.show();
            } catch (Exception e) {
                adCallback.onNextAction();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Dialog finalDialog = dialog;
        new Handler().postDelayed(() -> {
            if(splashAd != null) {
                splashAd.setFullScreenContentCallback(
                        new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                adCallback.onAdClosed();
                                splashAd = null;
                                isShowingAd = false;
                                if (finalDialog != null && !currentActivity.isDestroyed()) {
                                    try {
                                        finalDialog.dismiss();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                adCallback.onAdFailedToShow(adError);
                                isShowingAd = false;
                                dismissDialogLoading();
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                adCallback.onAdImpression();
                                isShowingAd = true;
                                splashAd = null;
                            }


                            @Override
                            public void onAdClicked() {
                                super.onAdClicked();
                                NarayanLogEventManager.logClickAdsEvent(context, splashAdId);
                                adCallback.onAdClicked();
                            }
                        });
                splashAd.show(currentActivity);
            }
        }, 800);
    }

    public void onCheckShowAppOpenSplashWhenFail(AppCompatActivity activity, AdCallback callback, int timeDelay) {
        new Handler(activity.getMainLooper()).postDelayed(() -> {
            if (splashAd != null && !isShowingAd()) {
                showAppOpenSplash(activity, new AdCallback() {
                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        callback.onNextAction();
                        splashAd = null;
                    }

                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        callback.onAdClosed();
                        splashAd = null;
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        callback.onAdFailedToShow(adError);
                        splashAd = null;
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        callback.onAdImpression();
                        splashAd = null;
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        callback.onAdClicked();
                    }
                });
            }
        }, timeDelay);
    }

    Runnable runnableTimeout = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "timeout load ad ");
            isTimeout = true;
            enableScreenContentCallback = false;
            if (fullScreenContentCallback != null) {
                fullScreenContentCallback.onAdDismissedFullScreenContent();
            }
        }
    };

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onResume() {
        if (!isAppResumeEnabled) {
            Log.e(TAG, "onResume: app resume is disabled");
            return;
        }

        if (isInterstitialShowing){
            Log.e(TAG, "onResume: interstitial is showing");
            return;
        }

        if (disableAdResumeByClickAction){
            Log.e(TAG, "onResume:ad resume disable ad by action");
            disableAdResumeByClickAction = false;
            return;
        }

        for (Class activity : disabledAppOpenList) {
            if (activity.getName().equals(currentActivity.getClass().getName())) {
                Log.e(TAG, "onStart: activity is disabled");
                return;
            }
        }

        if (splashActivity != null && splashActivity.getName().equals(currentActivity.getClass().getName())) {
            String adId = splashAdId;
            if (adId == null) {
                Log.e(TAG, "splash ad id must not be null");
            }
            Log.e(TAG, "onStart: load and show splash ads");
            loadAndShowSplashAds(adId);
            return;
        }

        Log.e(TAG, "onStart: show resume ads :"+ currentActivity.getClass().getName());
        showAdIfAvailable(false);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        Log.e(TAG, "onStop: app stop");

    }
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        Log.e(TAG, "onPause");
    }

    private void dismissDialogLoading() {
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

