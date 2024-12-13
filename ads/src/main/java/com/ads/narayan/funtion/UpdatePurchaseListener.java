package com.ads.narayan.funtion;

import com.android.billingclient.api.BillingResult;

public interface UpdatePurchaseListener {
    void onUpdateFinished(BillingResult billingResult);
}
