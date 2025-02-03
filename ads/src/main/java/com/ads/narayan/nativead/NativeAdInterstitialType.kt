package com.ads.narayan.nativead

enum class NativeAdInterstitialType(var id: Int) {
    WEBSITE(0),
    APP_STORE(1);
//    FULL_SCREEN(2);

    companion object {

        @JvmStatic
        fun fromId(value: Int): NativeAdInterstitialType {
            return entries.firstOrNull { it.id == value } ?: WEBSITE
        }
    }
}