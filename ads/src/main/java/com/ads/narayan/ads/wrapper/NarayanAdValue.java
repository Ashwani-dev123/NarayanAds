package com.ads.narayan.ads.wrapper;

import com.applovin.mediation.MaxAd;
import com.google.android.gms.ads.AdValue;

public class NarayanAdValue {
    private AdValue admobAdValue;
    private MaxAd  maxAdValue;

    public NarayanAdValue(MaxAd maxAdValue) {
        this.maxAdValue = maxAdValue;
    }

    public NarayanAdValue(AdValue admobAdValue) {
        this.admobAdValue = admobAdValue;
    }

    public AdValue getAdmobAdValue() {
        return admobAdValue;
    }

    public MaxAd getMaxAdValue() {
        return maxAdValue;
    }
}
