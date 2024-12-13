package com.ads.narayan.funtion;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;

public interface UpdatePurchaseListener {
    void onUpdateFinished(Purchase purchaseF);
}
