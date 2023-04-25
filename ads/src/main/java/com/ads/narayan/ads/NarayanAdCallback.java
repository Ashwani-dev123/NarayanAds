package com.ads.narayan.ads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ads.narayan.ads.wrapper.NarayanAdError;
import com.ads.narayan.ads.wrapper.NarayanInterstitialAd;
import com.ads.narayan.ads.wrapper.NarayanNativeAd;
import com.ads.narayan.ads.wrapper.NarayanRewardItem;

public class NarayanAdCallback {
    public void onNextAction() {
    }

    public void onAdClosed() {
    }

    public void onAdFailedToLoad(@Nullable NarayanAdError adError) {
    }

    public void onAdFailedToShow(@Nullable NarayanAdError adError) {
    }

    public void onAdLeftApplication() {
    }

    public void onAdLoaded() {

    }

    // ad splash loaded when showSplashIfReady = false
    public void onAdSplashReady() {

    }

    public void onInterstitialLoad(@Nullable NarayanInterstitialAd interstitialAd) {

    }

    public void onAdClicked() {
    }

    public void onAdImpression() {
    }


    public void onNativeAdLoaded(@NonNull NarayanNativeAd nativeAd) {

    }

    public void onUserEarnedReward(@NonNull NarayanRewardItem rewardItem) {

    }

    public void onInterstitialShow() {

    }

}
