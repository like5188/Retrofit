package com.like.retrofit.livedata

import androidx.lifecycle.MutableLiveData
import retrofit2.Call

/**
 * 持有[Call]引用的LiveData，可以用于取消网络请求
 */
open class CallLiveData<T>(private val call: Call<*>? = null) : MutableLiveData<T>() {
    @Volatile
    var isCanceled = false

    open fun cancel() {
        isCanceled = true
        call?.also {
            if (!it.isCanceled) {
                it.cancel()
            }
        }
    }

}