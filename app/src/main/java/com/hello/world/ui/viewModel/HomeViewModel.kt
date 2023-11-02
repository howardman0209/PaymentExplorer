package com.hello.world.ui.viewModel

import android.util.Log
import com.hello.world.model.PostMessageRequest
import com.hello.world.network.ApiManager
import com.hello.world.network.GatewayService
import com.hello.world.ui.base.BaseViewModel
import com.hello.world.util.DebugPanelManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class HomeViewModel : BaseViewModel() {

    fun sendRequest(path: String) {
        disposableList.add(
            ApiManager.create<GatewayService>(path)
                .postMessage(PostMessageRequest("Hello World"))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { showLoadingIndicator(false) }
                .subscribe({
                    Log.d("sendRequest", "it")
                    DebugPanelManager.log("ApiManager - sendRequest - response: ${it.body()}")
                }, {
                    Log.d("sendRequest", "Exception:${it.message}")
                    DebugPanelManager.log("ApiManager - sendRequest - Exception: ${it.message}")
                })
        )
    }
}