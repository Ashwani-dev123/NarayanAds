package com.ads.narayan.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ads.narayan.BuildConfig;
import com.ads.narayan.admob.Admob;
import com.ads.narayan.ads.wrapper.NarayanAdError;
import com.ads.narayan.ads.wrapper.NarayanInterstitialAd;
import com.ads.narayan.ads.wrapper.NarayanNativeAd;
import com.ads.narayan.billing.AppPurchase;
import com.ads.narayan.funtion.AdCallback;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;

public class PreloadAdsUtils {
    public static final String TAG = "PreloadAdsUtils";
    public static PreloadAdsUtils instance;

    public StorageCommon storageCommon=new StorageCommon();
    

    public static PreloadAdsUtils getInstance() {
        if (instance == null) {
            instance = new PreloadAdsUtils();
        }
        return instance;
    }

    public int loadTimesFailHigh = 0;
    public int loadTimesFailMedium = 0;
    public int loadTimesFailNormal = 0;
    public final int limitLoad = 2;


    public void loadInterSameTime(final Context context, String idAdInterPriority, String idAdInterNormal, NarayanAdCallback adListener) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        loadTimesFailHigh = 0;
        loadTimesFailNormal = 0;
        if (storageCommon.interPriority == null) {
            loadInterPriority(context, idAdInterPriority, adListener);
        }
        if (storageCommon.interNormal == null) {
            loadInterNormal(context, idAdInterNormal, adListener);
        }
    }

    public void loadInterNormal(Context context, String idAdInterNormal, NarayanAdCallback adListener) {
        Log.e(TAG, "loadInterNormal: ");
        NarayanAd.getInstance().getInterstitialAds(context, idAdInterNormal, new NarayanAdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable NarayanInterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                adListener.onInterstitialLoad(interstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@Nullable NarayanAdError adError) {
                super.onAdFailedToLoad(adError);
                Log.e(TAG, "onAdFailedToLoad: Normal");
                if (loadTimesFailNormal < limitLoad) {
                    loadTimesFailNormal++;
                    loadInterNormal(context, idAdInterNormal, adListener);
                }
            }
        });
    }

    public void loadInterPriority(Context context, String idAdInterPriority, NarayanAdCallback adListener) {
        Log.e(TAG, "loadInterPriority: ");
        NarayanAd.getInstance().getInterstitialAds(context, idAdInterPriority, new NarayanAdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable NarayanInterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                adListener.onInterstitialLoad(interstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@Nullable NarayanAdError adError) {
                super.onAdFailedToLoad(adError);
                Log.e(TAG, "onAdFailedToLoad: Priority");
                if (loadTimesFailHigh < limitLoad) {
                    loadTimesFailHigh++;
                    loadInterPriority(context, idAdInterPriority, adListener);
                }
            }
        });
    }

    public void showInterSameTime(
            Context context,
            NarayanInterstitialAd interPriority,
            NarayanInterstitialAd interNormal,
            Boolean reload,
            AdsInterCallBack adCallback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adCallback != null) {
                adCallback.onNextAction();
            }
            return;
        }
        if (interPriority != null) {
            Log.e(TAG, "showInterSameTime: Ad priority");
            NarayanAd.getInstance().forceShowInterstitial(
                    context,
                    interPriority,
                    new NarayanAdCallback() {
                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();
                            adCallback.onAdClosed();
                        }

                        @Override
                        public void onNextAction() {
                            super.onNextAction();
                            adCallback.onNextAction();
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            adCallback.onAdClicked();
                        }

                        @Override
                        public void onInterstitialShow() {
                            super.onInterstitialShow();
                            adCallback.onInterstitialPriorityShowed();
                        }
                    },
                    reload);
        } else if (interNormal != null) {
            Log.e(TAG, "showInterSameTime: Ad normal");
            NarayanAd.getInstance().forceShowInterstitial(
                    context,
                    interNormal,
                    new NarayanAdCallback() {
                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();
                            adCallback.onAdClosed();
                        }

                        @Override
                        public void onNextAction() {
                            super.onNextAction();

                            adCallback.onNextAction();
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            adCallback.onAdClicked();
                        }

                        public void onInterstitialShow() {
                            super.onInterstitialShow();
                            adCallback.onInterstitialNormalShowed();
                        }
                    },
                    reload);
        } else {
            adCallback.onNextAction();
        }
    }

    public void setLayoutNative(int layoutNativeCustom){

        Log.e("ashdvadjasd", "getStorageCommon: "+storageCommon );
        Log.e("ashdvadjasd", "nativeAdHigh: "+storageCommon.nativeAdHigh );
        Log.e("ashdvadjasd", "nativeAdMedium: "+storageCommon.nativeAdMedium );
        Log.e("ashdvadjasd", "nativeAdNormal: "+storageCommon.nativeAdNormal );

        if (storageCommon.nativeAdHigh != null){
            storageCommon.apNativeAdHigh =
                    new NarayanNativeAd(layoutNativeCustom, storageCommon.nativeAdHigh);
        } else if (storageCommon.nativeAdMedium != null){
            storageCommon.apNativeAdMedium =
                    new NarayanNativeAd(layoutNativeCustom, storageCommon.nativeAdMedium);
        } else if (storageCommon.nativeAdNormal != null){
            storageCommon.apNativeAdNormal =
                    new NarayanNativeAd(layoutNativeCustom, storageCommon.nativeAdNormal);
        }
    }

    public void preLoadNativeSameTime(final Activity activity) {
        if (AppPurchase.getInstance().isPurchased(activity)) {
            return;
        }
        loadTimesFailHigh = 0;
        loadTimesFailMedium = 0;
        loadTimesFailNormal = 0;

        PreloadNativeCallback callBack = new PreloadNativeCallback() {
            @Override
            public void onNativeAdLoaded(NativeAd nativeAd) {
                storageCommon.nativeAdNormal = nativeAd;
            }

            @Override
            public void onNativeHighAdLoaded(NativeAd nativeAd) {
                storageCommon.nativeAdHigh = nativeAd;
            }

            @Override
            public void onNativeMediumAdLoaded(NativeAd nativeAd) {
                storageCommon.nativeAdMedium = nativeAd;
            }

            @Override
            public void onNativeAdShow() {}

            @Override
            public void onNativeHighAdShow() {}

            @Override
            public void onNativeMediumAdShow() {}
        };

        if (storageCommon.nativeAdHigh == null) {
            loadNativeHigh(activity, BuildConfig.ad_native_priority, callBack);
        }
        if (storageCommon.nativeAdMedium == null) {
            loadNativeMedium(activity, BuildConfig.ad_native_medium, callBack);
        }
        if (storageCommon.nativeAdNormal == null) {
            loadNativeNormal(activity, BuildConfig.ad_native, callBack);
        }
    }

    public void loadNativeHigh(
            Activity activity,
            String idNativeHigh,
            PreloadNativeCallback callBack) {
        Admob.getInstance().loadNativeAd(
                activity,
                idNativeHigh,
                new AdCallback(){
                    @Override
                    public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                        super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                        Log.d(TAG, "loadNativeHigh");
                        callBack.onNativeHighAdLoaded(unifiedNativeAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        Log.d(TAG, "FailToLoadNativeHigh");
                        if (loadTimesFailHigh < limitLoad) {
                            loadTimesFailHigh++;
                            loadNativeHigh(activity, idNativeHigh, callBack);
                        }
                    }
                }
        );
    }

    public void loadNativeMedium(
            Activity activity,
            String idNativeMedium,
            PreloadNativeCallback callBack) {
        Admob.getInstance().loadNativeAd(
                activity,
                idNativeMedium,
                new AdCallback(){
                    @Override
                    public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                        super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                        Log.d(TAG, "loadNativeMedium");
                        callBack.onNativeMediumAdLoaded(unifiedNativeAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        Log.d(TAG, "FailToLoadNativeMedium");
                        if (loadTimesFailMedium < limitLoad) {
                            loadTimesFailMedium++;
                            loadNativeMedium(activity, idNativeMedium, callBack);
                        }
                    }
                }
        );
    }

    public void loadNativeNormal(
            Activity activity,
            String idNativeNormal,
            PreloadNativeCallback callBack) {
        Admob.getInstance().loadNativeAd(
                activity,
                idNativeNormal,
                new AdCallback(){
                    @Override
                    public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                        super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                        Log.d(TAG, "loadNativeNormal");
                        callBack.onNativeAdLoaded(unifiedNativeAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                        Log.d(TAG, "FailToLoadNativeNormal");
                        if (loadTimesFailNormal < limitLoad) {
                            loadTimesFailNormal++;
                            loadNativeNormal(activity, idNativeNormal, callBack);
                        }
                    }
                }
        );
    }

    public void showPreNativeSametime(
            Activity activity,
            FrameLayout adPlaceHolder,
            ShimmerFrameLayout containerShimmerLoading) {

        PreloadNativeCallback callback = new PreloadNativeCallback() {
            @Override
            public void onNativeAdLoaded(NativeAd nativeAd) {

            }

            @Override
            public void onNativeHighAdLoaded(NativeAd nativeAd) {

            }

            @Override
            public void onNativeMediumAdLoaded(NativeAd nativeAd) {

            }

            @Override
            public void onNativeAdShow() {
                storageCommon.apNativeAdNormal = null;
                storageCommon.nativeAdNormal = null;
                PreloadAdsUtils.getInstance().preLoadNativeSameTime(activity);
            }

            @Override
            public void onNativeHighAdShow() {
                storageCommon.nativeAdHigh = null;
                storageCommon.apNativeAdHigh = null;
                PreloadAdsUtils.getInstance().preLoadNativeSameTime(activity);
            }

            @Override
            public void onNativeMediumAdShow() {
                storageCommon.nativeAdMedium = null;
                storageCommon.apNativeAdMedium = null;
                PreloadAdsUtils.getInstance().preLoadNativeSameTime(activity);
            }
        };

        if (storageCommon.apNativeAdHigh != null) {
            Log.d(TAG, "showPreNativeSametime: nativeAdHigh");
            NarayanAd.getInstance().populateNativeAdView(
                    activity,
                    storageCommon.apNativeAdHigh,
                    adPlaceHolder,
                    containerShimmerLoading
            );
            callback.onNativeHighAdShow();
        } else if (storageCommon.apNativeAdMedium != null) {
            Log.d(TAG, "showPreNativeSametime: nativeAdMedium");
            NarayanAd.getInstance().populateNativeAdView(
                    activity,
                    storageCommon.apNativeAdMedium,
                    adPlaceHolder,
                    containerShimmerLoading
            );
            callback.onNativeMediumAdShow();
        } else if (storageCommon.apNativeAdNormal != null) {
            Log.d(TAG, "showPreNativeSametime: nativeAdNormal");
            NarayanAd.getInstance().populateNativeAdView(
                    activity,
                    storageCommon.apNativeAdNormal,
                    adPlaceHolder,
                    containerShimmerLoading
            );
            callback.onNativeAdShow();
        } else {
            adPlaceHolder.setVisibility(View.GONE);
        }
    }

}
