package com.mobile.gateway.extension

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.common.BitMatrix
import kotlin.math.max

fun BitMatrix.toBitmap(): Bitmap? {
    val w = this.width
    val h = this.height
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        val offset = y * w
        for (x in 0 until w) {
            pixels[offset + x] = if (this[x, y]) Color.BLACK else Color.WHITE
        }
    }
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, max(w, h), 0, 0, w, h)
    return bitmap
}