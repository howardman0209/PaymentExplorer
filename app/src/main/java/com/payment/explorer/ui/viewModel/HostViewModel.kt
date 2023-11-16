package com.payment.explorer.ui.viewModel

import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.payment.explorer.model.PostMessageRequest
import com.payment.explorer.network.ApiManager
import com.payment.explorer.network.GatewayService
import com.payment.explorer.ui.base.BaseViewModel
import com.payment.explorer.util.DebugPanelManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class HostViewModel : BaseViewModel() {
    val ipAddress = ObservableField<String>()
    val serverStarted = ObservableField(false)
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