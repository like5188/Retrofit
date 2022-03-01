package com.like.retrofit.interceptor

import android.app.Application
import com.like.retrofit.util.NetWorkUtils
import com.like.retrofit.util.NetworkException
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 网络是否连接拦截器
 * 如果没有连接，会抛出[NetworkException]
 */
class NetworkMonitorInterceptor(private val application: Application) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!NetWorkUtils.isConnected(application)) {// 无网时
            throw NetworkException()
        }
        return chain.proceed(chain.request())
    }

}