package com.hello.world.network

import com.hello.world.model.PostMessageRequest
import com.hello.world.model.PostMessageResponse
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


const val messageAPIPath = "message"

interface GatewayService {
    @POST(messageAPIPath)
    fun postMessage(@Body data: PostMessageRequest): Observable<Response<PostMessageResponse>>
}