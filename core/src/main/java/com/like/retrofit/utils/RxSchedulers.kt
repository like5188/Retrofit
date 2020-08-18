package com.like.retrofit.utils

import io.reactivex.FlowableTransformer
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * 返回经过线程包装Observable、Flowable。

 * @author like
 * @version 1.0
 * @created at 2017/3/25 20:10
 */
object RxSchedulers {

    @JvmStatic
    fun <T> observableIo2Main(): ObservableTransformer<T, T> = ObservableTransformer {
        it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    @JvmStatic
    fun <T> flowableIo2Main(): FlowableTransformer<T, T> = FlowableTransformer {
        it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

}
