package com.ads.narayan.nativeadsutils.blurEffect

import android.graphics.Bitmap

interface BlurProcess {
    fun blur(original: Bitmap, radius: Float): Bitmap?
}