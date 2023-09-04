package com.ads.narayan.ads;

import androidx.lifecycle.MutableLiveData;

import com.ads.narayan.ads.wrapper.NarayanInterstitialAd;
import com.ads.narayan.ads.wrapper.NarayanNativeAd;
import com.google.android.gms.ads.nativead.NativeAd;

public class StorageCommon {
    public MutableLiveData<NarayanNativeAd> nativeAdsLanguage = new MutableLiveData<>();
    public NarayanInterstitialAd interPriority;
    public NarayanInterstitialAd interNormal;

    public NarayanNativeAd apNativeAdHigh;
    public NarayanNativeAd apNativeAdMedium;
    public NarayanNativeAd apNativeAdNormal;

    public NativeAd nativeAdHigh;
    public NativeAd nativeAdMedium;
    public NativeAd nativeAdNormal;
}
