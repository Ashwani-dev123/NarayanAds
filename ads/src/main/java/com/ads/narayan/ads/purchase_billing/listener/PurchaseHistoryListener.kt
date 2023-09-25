package com.ads.narayan.ads.purchase_billing.listener

import com.ads.narayan.ads.purchase_billing.NarayanBillingResult
import com.ads.narayan.ads.purchase_billing.PurchaseHistory


fun interface PurchaseHistoryListener {

    fun onCheckPurchaseHistory(result: NarayanBillingResult, history: List<PurchaseHistory>)

}