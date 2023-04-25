package com.ads.narayan.ads.wrapper;


public abstract class NarayanAdBase {
    protected StatusAd status = StatusAd.AD_INIT;

    public NarayanAdBase(StatusAd status) {
        this.status = status;
    }

    public NarayanAdBase() {
    }

    public StatusAd getStatus() {
        return status;
    }

    public void setStatus(StatusAd status) {
        this.status = status;
    }


    abstract boolean isReady();

    public boolean isNotReady(){
        return !isReady();
    }

    public boolean isLoading(){
        return status == StatusAd.AD_LOADING;
    }
    public boolean isLoadFail(){
        return status == StatusAd.AD_LOAD_FAIL;
    }
}
