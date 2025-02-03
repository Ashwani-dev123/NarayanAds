@file:Suppress("unused")

package com.ads.narayan.nativeadsutils

import android.util.Log


internal var isEnableDebugMode: Boolean = true
internal var isPurchaseHistoryLogEnable: Boolean = false

fun getDebugModeStatus(): Boolean = isEnableDebugMode
fun getPurchaseHistoryLogStatus(): Boolean = isPurchaseHistoryLogEnable

internal fun logD(tag: String, message: String) {
    if (isEnableDebugMode) {
        Log.d(tag, message)
    }
}

internal fun logI(tag: String, message: String) {
    if (isEnableDebugMode) {
        Log.i(tag, message)
    }
}

internal fun logE(tag: String, message: String) {
    if (isEnableDebugMode) {
        Log.e(tag, message)
    }
}

internal fun logW(tag: String, message: String) {
    if (isEnableDebugMode) {
        Log.w(tag, message)
    }
}