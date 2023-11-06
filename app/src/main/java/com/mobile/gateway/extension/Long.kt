package com.mobile.gateway.extension

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toDateString(pattern: String): String {
    return SimpleDateFormat(pattern, Locale.ENGLISH).format(Date(this))
}