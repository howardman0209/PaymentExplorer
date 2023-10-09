package com.golden.template.extension

import com.google.gson.Gson

fun <T> T.toSerializedMap(): Map<String, Any> {
    val jsonStr = Gson().toJson(this)
    return jsonStr.toSerializedMap()
}