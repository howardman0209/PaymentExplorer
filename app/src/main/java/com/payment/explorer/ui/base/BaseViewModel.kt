package com.payment.explorer.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.Disposable
import kotlin.collections.ArrayList

open class BaseViewModel : ViewModel() {
    val isShowLoading: MutableLiveData<Boolean> = MutableLiveData()
    val disposableList: ArrayList<Disposable> = arrayListOf()
    val apiError = MutableLiveData<String>()

    fun showLoadingIndicator(showLoading: Boolean) {
        isShowLoading.postValue(showLoading)
    }

    fun getDisposableList(): List<Disposable> = disposableList

    fun getCurrentTimestamp(): Long {
        return (System.currentTimeMillis() / 1000)
    }
}