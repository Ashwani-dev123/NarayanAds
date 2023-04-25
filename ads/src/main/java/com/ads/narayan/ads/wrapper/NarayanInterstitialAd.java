package com.ads.narayan.ads.wrapper;

import com.applovin.mediation.ads.MaxInterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;

public class NarayanInterstitialAd extends NarayanAdBase {
    private InterstitialAd interstitialAd;
    private MaxInterstitialAd maxInterstitialAd;

    public NarayanInterstitialAd(StatusAd status) {
        super(status);
    }

    public NarayanInterstitialAd() {
    }

    public NarayanInterstitialAd(MaxInterstitialAd maxInterstitialAd) {
        this.maxInterstitialAd = maxInterstitialAd;
        status = StatusAd.AD_LOADED;
    }

    public NarayanInterstitialAd(InterstitialAd interstitialAd) {
        this.interstitialAd = interstitialAd;
        status = StatusAd.AD_LOADED;
    }


    public void setInterstitialAd(InterstitialAd interstitialAd) {
        this.interstitialAd = interstitialAd;
        status = StatusAd.AD_LOADED;
    }

    public void setMaxInterstitialAd(MaxInterstitialAd maxInterstitialAd) {
        this.maxInterstitialAd = maxInterstitialAd;
        status = StatusAd.AD_LOADED;
    }

    @Override
    public boolean isReady(){
        if (maxInterstitialAd!=null && maxInterstitialAd.isReady())
            return true;
        return interstitialAd != null;
    }


    public InterstitialAd getInterstitialAd() {
        return interstitialAd;
    }

    public MaxInterstitialAd getMaxInterstitialAd() {
        return maxInterstitialAd;
    }
}
