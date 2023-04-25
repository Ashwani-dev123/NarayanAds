package com.ads.narayan.application;

import androidx.multidex.MultiDexApplication;

import com.ads.narayan.config.NarayanAdConfig;
import com.ads.narayan.util.AppUtil;
import com.ads.narayan.util.SharePreferenceUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AdsMultiDexApplication extends MultiDexApplication {

    protected NarayanAdConfig narayanAdConfig;
    protected List<String> listTestDevice ;
    @Override
    public void onCreate() {
        super.onCreate();
        listTestDevice = new ArrayList<String>();
        narayanAdConfig = new NarayanAdConfig(this);
        if (SharePreferenceUtils.getInstallTime(this) == 0) {
            SharePreferenceUtils.setInstallTime(this);
        }
        AppUtil.currentTotalRevenue001Ad = SharePreferenceUtils.getCurrentTotalRevenue001Ad(this);
    }


}
