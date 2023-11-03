package com.hello.world.ui.viewModel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.hello.world.model.PostMessageRequest
import com.hello.world.network.ApiManager
import com.hello.world.network.GatewayService
import com.hello.world.ui.base.BaseViewModel
import com.hello.world.util.DebugPanelManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class HomeViewModel : BaseViewModel() {

    fun sendRequestWithRetrofit(path: String) {
        disposableList.add(
            ApiManager.create<GatewayService>(path)
                .postMessage(PostMessageRequest("With Retrofit"))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { showLoadingIndicator(false) }
                .subscribe({
                    if (it.code() == 200) {
                        DebugPanelManager.log("Retrofit - sendRequest - Success: ${it.body()}")
                    } else {
                        DebugPanelManager.log("Retrofit - sendRequest - Fail: ${it.code()}")
                    }
                }, {
                    DebugPanelManager.log("Retrofit - sendRequest - Exception: ${it.message}")
                })
        )
    }

    fun sendRequest(
        path: String,
        endPoint: String,
    ) {
        val client = OkHttpClient()
        val requestBody = "{\"message\": \"Without Retrofit\"}".toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$path/$endPoint")
            .method("POST", requestBody)
            .build()
        val call = client.newCall(request)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = call.execute()
                if (response.code == 200) {
                    DebugPanelManager.log("OkHttpClient - sendRequest - Success: ${response.body?.string()}")
                } else {
                    DebugPanelManager.log("OkHttpClient - sendRequest - Fail: ${response.code}")
                }
            } catch (ex: Exception) {
                DebugPanelManager.log("OkHttpClient - sendRequest - Exception: ${ex.message}")
            }
        }
    }
}