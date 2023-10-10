package com.hello.world.ui.base

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hello.world.util.DATE_TIME_DISPLAY_PATTERN_FULL
import io.reactivex.disposables.Disposable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

open class BaseViewModel : ViewModel() {
    val isShowLoading: MutableLiveData<Boolean> = MutableLiveData()
    val disposableList: ArrayList<Disposable> = arrayListOf()
    val apiError = MutableLiveData<String>()

    fun showLoadingIndicator(showLoading: Boolean) {
        isShowLoading.postValue(showLoading)
    }

    fun getDisposableList(): List<Disposable> = disposableList

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDateTime(): String {
        val formatter = DateTimeFormatter.ofPattern(DATE_TIME_DISPLAY_PATTERN_FULL, Locale.ENGLISH)
        return LocalDateTime.now().format(formatter)
    }

    fun getCurrentTimestamp(): Long {
        return (System.currentTimeMillis() / 1000)
    }
}