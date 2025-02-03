package com.ads.narayan.nativead

enum class NativeAdType(var id: Int) {
    BIG(0),
    MEDIUM(1),
    INTERSTITIAL_NATIVE(2),
    CUSTOM(3);

    //    FULL_SCREEN(2),
    companion object {

        @JvmStatic
        fun fromId(value: Int): NativeAdType {
            return entries.firstOrNull { it.id == value } ?: BIG
        }
    }
}