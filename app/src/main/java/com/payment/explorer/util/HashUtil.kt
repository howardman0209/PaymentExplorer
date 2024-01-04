package com.payment.explorer.util

import android.util.Log
import com.payment.explorer.extension.hexToByteArray
import com.payment.explorer.extension.toHexString
import com.payment.explorer.model.HashMethod
import java.security.MessageDigest

object HashUtil {
    fun getHash(data: String, hashMethod: HashMethod): String {
        val md = MessageDigest.getInstance(hashMethod.algorithm)
        val hash = md.digest(data.toByteArray()).toHexString()
        Log.d("HashUtil", "hash: $hash")
        return hash
    }

    fun hexGetHash(data: String, hashMethod: HashMethod): String {
        val md = MessageDigest.getInstance(hashMethod.algorithm)
        val hash = md.digest(data.hexToByteArray()).toHexString()
        Log.d("HashUtil", "hash: $hash")
        return hash
    }

    fun getHash(data: ByteArray, hashMethod: HashMethod): ByteArray {
        val md = MessageDigest.getInstance(hashMethod.algorithm)
        return md.digest(data)
    }
}