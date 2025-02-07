@file:Suppress("unused")

package com.ads.narayan.nativeadsutils

import com.google.android.gms.ads.AdListener


data class AdStatusModel<T>(
    var loadedAd: T? = null,
    var adID: String = "",
    var listener: NativeAdLoadCallback<T>? = null,
    var isAdLoadingRunning: Boolean = false,
    var defaultAdListener: AdListener? = null
)