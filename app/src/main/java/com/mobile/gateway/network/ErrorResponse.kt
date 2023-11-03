package com.mobile.gateway.network

import android.content.Context

data class ErrorResponse(
    private val message: Any? = null,
    val result: String? = null
) {
    /**
     * Get message WITHOUT context for display purpose is UNEXPECTED
     * Message can be string res, get display message with context
     */
    fun getDisplayMessage(context: Context?): String {
        return when (message) {
            is Double -> {
                //Strange. Even when you put stringId which is Int it turns out become double.
                context?.getString(message.toInt()) ?: message.toString()
            }
            is Int -> {
                context?.getString(message) ?: message.toString()
            }
            else -> {
                message?.toString().orEmpty()
            }
        }
    }
}