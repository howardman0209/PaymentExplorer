package com.hello.world.network

import android.util.Log
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object ApiManager {
    const val GATEWAY_CONNECT_TIMEOUT  = 60000L
    const val GATEWAY_READ_WRITE_TIMEOUT  = 60000L
    val headers = mapOf("accept" to "*/*")

    inline fun <reified T> create(baseUrl: String, retrofitEventListener: EventListener? = null): T {
        val loggingInterceptor = HttpLoggingInterceptor { message -> Log.d("ApiManager", message) }
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(ParseResponseInterceptor())
            .addInterceptor(loggingInterceptor)
            .apply {
                retrofitEventListener?.let {
                    eventListener(it)
                }
            }
            .connectTimeout(GATEWAY_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(GATEWAY_READ_WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()

        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(ScalarsConverterFactory.create()) //API like reporting return plain text
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(clientBuilder)
            .build()

        return retrofit.create(T::class.java)
    }
}