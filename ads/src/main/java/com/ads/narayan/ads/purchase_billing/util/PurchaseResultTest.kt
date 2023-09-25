package com.ads.narayan.ads.purchase_billing.util

import com.ads.narayan.ads.purchase_billing.NarayanBillingResult
import com.ads.narayan.ads.purchase_billing.PurchaseIap

data class PurchaseResultTest(
    val result: NarayanBillingResult,
    val purchases: List<PurchaseIap>
)