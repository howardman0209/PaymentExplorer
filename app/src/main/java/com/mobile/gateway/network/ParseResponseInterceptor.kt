package com.mobile.gateway.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response

/**
 * Handle both gateway & HTTP error
 * If there is error body, handle error with "result"
 * @see ErrorResponse
 * Else handle error with HTTP code
 */
class ParseResponseInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val responseBody = response.body

        val gson = Gson()

        if (responseBody != null) {
            if (!response.isSuccessful) {
                val errorResponse = try {
                    gson.fromJson<ErrorResponse>(responseBody.charStream(), object : TypeToken<ErrorResponse>() {}.type)
                } catch (ex: Exception) {
                    null
                }
                //if response can be casted to ErrorResponse, use result to handle error, else use http code to handle error
                //message here is not used for display, pass null into getDisplayMessage
                throw ApiException(errorResponse?.result ?: response.code.toString(), errorResponse?.getDisplayMessage(null))
            }
        }

        return response
    }
}

class ApiException(val errorCode: String?, override val message: String?) : Exception(message)