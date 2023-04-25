package com.ads.narayan.ads.bannerAds;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ads.narayan.R;
import com.ads.narayan.admob.Admob;
import com.ads.narayan.ads.NarayanAd;
import com.ads.narayan.ads.NarayanAdCallback;
import com.ads.narayan.funtion.AdCallback;


public class NarayanBannerAdView extends RelativeLayout {

    private String TAG = "NarayanBannerAdView";

    public NarayanBannerAdView(@NonNull Context context) {
        super(context);
        init();
    }

    public NarayanBannerAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public NarayanBannerAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    public NarayanBannerAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.layout_banner_control, this);
    }

    public void loadBanner(Activity activity, String idBanner) {
        loadBanner(activity, idBanner, new NarayanAdCallback());
    }

    public void loadBanner(Activity activity, String idBanner, NarayanAdCallback narayanAdCallback) {
        NarayanAd.getInstance().loadBanner(activity, idBanner, narayanAdCallback);
    }

    public void loadInlineBanner(Activity activity, String idBanner, String inlineStyle) {
        Admob.getInstance().loadInlineBanner(activity, idBanner, inlineStyle);
    }

    public void loadInlineBanner(Activity activity, String idBanner, String inlineStyle, AdCallback adCallback) {
        Admob.getInstance().loadInlineBanner(activity, idBanner, inlineStyle, adCallback);
    }

    public void loadBannerFragment(Activity activity, String idBanner) {
        NarayanAd.getInstance().loadBannerFragment(activity, idBanner, getRootView());
    }

    public void loadBannerFragment(Activity activity, String idBanner, AdCallback adCallback) {
        NarayanAd.getInstance().loadBannerFragment(activity, idBanner, getRootView(), adCallback);
    }

    public void loadInlineBannerFragment(Activity activity, String idBanner, String inlineStyle) {
        Admob.getInstance().loadInlineBannerFragment(activity, idBanner, getRootView(), inlineStyle);
    }

    public void loadInlineBannerFragment(Activity activity, String idBanner, String inlineStyle, AdCallback adCallback) {
        Admob.getInstance().loadInlineBannerFragment(activity, idBanner, getRootView(), inlineStyle, adCallback);
    }
}