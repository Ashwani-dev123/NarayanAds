package com.ads.narayan.ads.wrapper;

import com.applovin.mediation.MaxReward;
import com.google.android.gms.ads.rewarded.RewardItem;

public class NarayanRewardItem {

    private RewardItem admobRewardItem;
    private MaxReward maxRewardItem;

    public NarayanRewardItem(MaxReward maxRewardItem) {
        this.maxRewardItem = maxRewardItem;
    }

    public NarayanRewardItem(RewardItem admobRewardItem) {
        this.admobRewardItem = admobRewardItem;
    }

    public RewardItem getAdmobRewardItem() {
        return admobRewardItem;
    }

    public void setAdmobRewardItem(RewardItem admobRewardItem) {
        this.admobRewardItem = admobRewardItem;
    }

    public MaxReward getMaxRewardItem() {
        return maxRewardItem;
    }

    public void setMaxRewardItem(MaxReward maxRewardItem) {
        this.maxRewardItem = maxRewardItem;
    }
}
