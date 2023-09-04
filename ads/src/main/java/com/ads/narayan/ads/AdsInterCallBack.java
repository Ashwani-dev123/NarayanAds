package com.ads.narayan.ads;

public interface AdsInterCallBack {

    void onInterstitialPriorityShowed();

    void onInterstitialNormalShowed();

    void onAdClosed();

    void onAdClicked();

    void onNextAction();
}
