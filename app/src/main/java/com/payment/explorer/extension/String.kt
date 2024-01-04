package com.payment.explorer.extension

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
import com.payment.explorer.model.BitwiseOperation
import com.payment.explorer.model.PaddingMethod
import java.math.BigInteger
import java.util.EnumMap
import kotlin.math.ceil

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

fun String.hexToBinary(): String {
    val binary = this.toLong(16).toString(2).uppercase()
    return binary.padStart(this.length * 4, '0')
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

fun String.hexBitwise(hex: String = "", operation: BitwiseOperation): String {
    val data = BigInteger(this, 16)
    val res = when (operation) {
        BitwiseOperation.XOR -> data.xor(BigInteger(hex, 16))
        BitwiseOperation.AND -> data.and(BigInteger(hex, 16))
        BitwiseOperation.OR -> data.or(BigInteger(hex, 16))
        BitwiseOperation.NOT -> data.xor(BigInteger("FF".padEnd(this.length, 'F'), 16))
    }
    return res.toString(16).uppercase().padStart(hex.length, '0')
}

fun String.applyPadding(paddingMethod: PaddingMethod): String {
    return when (paddingMethod) {
        PaddingMethod.ISO9797_M1 -> {
            val padLen = ceil(this.length.div(16.0)).toInt().times(16).let {
                if (it > 16) it else 16
            }
            this.padEnd(padLen, '0')
        }

        PaddingMethod.ISO9797_M2 -> {
            val padLen = ceil("${this}80".length.div(16.0)).toInt().times(16).let {
                if (it > 16) it else 16
            }
            "${this}80".padEnd(padLen, '0')
        }
    }
}