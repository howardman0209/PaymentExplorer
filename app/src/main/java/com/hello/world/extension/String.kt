package com.hello.world.extension

import android.graphics.Bitmap
import android.text.Editable
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.encoder.QRCode
import java.util.EnumMap

fun String.insert(insert: String, index: Int): String {
    val start = substring(0, index)
    val end = substring(index)
    return start + insert + end
}

fun String.hexToByteArray(): ByteArray {
    //Add leading zero in case of odd len
    val str = if (this.length and 1 == 1) {
        "0$this"
    } else
        this
    return str.trim().chunked(2).map { it.toInt(16).toByte() }.toByteArray()

}

fun String.hexToAscii(): String {
    require(length % 2 == 0) { "Input data have an even length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
        .toString(Charsets.ISO_8859_1)  // Or whichever encoding your input uses
}

fun String.asciiToHex(separator: String = " "): String {
    val output = StringBuilder("")
    this.forEach {
        output.append(it.code.toString(16))
        output.append(separator)
    }
    return output.toString()
}

fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

inline fun <reified T> String.toDataClass(): T {
    return Gson().fromJson(this, T::class.java)
}

fun String.toSerializedMap(): Map<String, Any> {
    return this.toDataClass<JsonObject>().toMap()
}

fun String.qrcodeDataToBitMatrix(margin: Int? = null, ecLevel: ErrorCorrectionLevel? = null, maskPattern: Int? = null, unitWidth: Int? = null): BitMatrix? {
    val hints: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
    margin?.also { hints[EncodeHintType.MARGIN] = 0 /* default = 4 */ }
    maskPattern?.also { if (QRCode.isValidMaskPattern(maskPattern)) hints[EncodeHintType.QR_MASK_PATTERN] = maskPattern }
    ecLevel?.also { hints[EncodeHintType.ERROR_CORRECTION] = ecLevel }

    // pre encode data to a qr code to get qr version
    val qrCodeVersion = Encoder.encode(this, ecLevel, hints).version
    hints[EncodeHintType.QR_VERSION] = qrCodeVersion.versionNumber
    val qrDimension = unitWidth?.let {
        qrCodeVersion.dimensionForVersion * unitWidth
    } ?: 300

    return try {
        MultiFormatWriter().encode(this, BarcodeFormat.QR_CODE, qrDimension, qrDimension, hints)
    } catch (iae: IllegalArgumentException) {
        // Unsupported format
        null
    }
}

fun String.qrcodeDataToBitmap(): Bitmap? {
    return this.qrcodeDataToBitMatrix()?.toBitmap()
}
