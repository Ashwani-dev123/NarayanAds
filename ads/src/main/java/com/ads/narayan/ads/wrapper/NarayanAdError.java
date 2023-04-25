package com.ads.narayan.ads.wrapper;

import com.applovin.mediation.MaxError;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.LoadAdError;

public class NarayanAdError {

    private MaxError maxError;
    private LoadAdError loadAdError;
    private AdError adError;
    private String message = "";

    public NarayanAdError(AdError adError) {
        this.adError = adError;
    }

    public NarayanAdError(LoadAdError loadAdError) {
        this.loadAdError = loadAdError;
    }

    public NarayanAdError(MaxError maxError) {
        this.maxError = maxError;
    }

    public NarayanAdError(String message) {
        this.message = message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage(){
        if (maxError!=null)
            return maxError.getMessage();
        if (loadAdError!=null)
            return loadAdError.getMessage();
        if (adError!=null)
            return adError.getMessage();
        if (!message.isEmpty())
            return message;
        return "unknown error";
    }
}
