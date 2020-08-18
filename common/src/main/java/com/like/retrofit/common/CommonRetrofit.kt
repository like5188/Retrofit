package com.like.retrofit.common

import com.google.gson.Gson
import com.like.retrofit.common.factory.LiveDataCallAdapterFactory
import com.like.retrofit.utils.OkHttpClientFactory
import com.like.retrofit.RequestConfig
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * 普通网络请求帮助类。
 */
class CommonRetrofit {
    var mRetrofit: Retrofit? = null

    fun init(requestConfig: RequestConfig, gson: Gson = Gson()): CommonRetrofit {
        mRetrofit = Retrofit.Builder()
            .client(
                OkHttpClientFactory.createOkHttpClientBuilder(requestConfig)
                    .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))// 添加日志打印
                    .build()
            )
            .baseUrl(requestConfig.baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())// 处理String和8种基本数据类型的情况。
            .addConverterFactory(GsonConverterFactory.create(gson))// 处理实体对象的情况。
            // Retrofit事先并不知道要使用哪个CallAdapterFactory，所以她是遍历所有的CallAdapterFactory，根据目标函数的返回值类型，让每个Factory都去尝试生产一个CallAdapter，哪个成功就用哪个。
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())// 支持接口返回类型转换：Call<T> 转换成 LiveData<ApiResponse<T>>
            .build()
        return this
    }

    /**
     * 获取自定义的api服务类实例
     */
    @Throws(UnsupportedOperationException::class)
    inline fun <reified T> getService(): T {
        val retrofit = mRetrofit ?: throw UnsupportedOperationException("you must call init() method first")
        return retrofit.create(T::class.java)
    }

}