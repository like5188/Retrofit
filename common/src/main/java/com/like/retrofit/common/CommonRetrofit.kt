package com.like.retrofit.common

import com.google.gson.Gson
import com.like.retrofit.RequestConfig
import com.like.retrofit.util.OkHttpClientFactory
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * 普通网络请求帮助类。
 */
class CommonRetrofit {
    var mRetrofit: Retrofit? = null

    fun init(requestConfig: RequestConfig, gsonConverterFactory: Converter.Factory = GsonConverterFactory.create(Gson())): CommonRetrofit {
        mRetrofit = Retrofit.Builder()
            .client(
                OkHttpClientFactory.createOkHttpClientBuilder(requestConfig)
                    .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))// 添加日志打印
                    .build()
            )
            .baseUrl(requestConfig.baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())// 处理String和8种基本数据类型的情况。
            .addConverterFactory(gsonConverterFactory)// 处理实体对象的情况。
            .build()
        return this
    }

    /**
     * 获取自定义的api服务类实例
     */
    @Throws(UnsupportedOperationException::class)
    inline fun <reified T> getService(): T {
        val retrofit =
            mRetrofit ?: throw UnsupportedOperationException("you must call init() method first")
        return retrofit.create(T::class.java)
    }

}