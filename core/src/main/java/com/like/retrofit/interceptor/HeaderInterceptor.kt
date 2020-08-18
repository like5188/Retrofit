package com.like.retrofit.interceptor

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * 自定义的添加公共 header 的拦截器
 */
abstract class HeaderInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val headerMap = getHeaderMap()
        if (headerMap.isNullOrEmpty()) {
            return chain.proceed(chain.request())
        }
        val requestBuilder: Request.Builder = chain.request().newBuilder()
        for ((key, value) in headerMap) {
            requestBuilder.addHeader(key, value)
        }
        return chain.proceed(requestBuilder.build())
    }

    abstract fun getHeaderMap(): Map<String, String>
}