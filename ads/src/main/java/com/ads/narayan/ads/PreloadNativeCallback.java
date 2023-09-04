package com.ads.narayan.ads;

import com.google.android.gms.ads.nativead.NativeAd;

public interface PreloadNativeCallback {
    void onNativeAdLoaded(NativeAd nativeAd);

    void onNativeHighAdLoaded(NativeAd nativeAd);

    void onNativeMediumAdLoaded(NativeAd nativeAd);

    void onNativeAdShow();

    void onNativeHighAdShow();

    void onNativeMediumAdShow();
}
