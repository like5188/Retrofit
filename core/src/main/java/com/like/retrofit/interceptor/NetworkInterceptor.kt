package com.like.retrofit.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 自定义的网络拦截器
 *
 * 客户端请求时，header传入想要的缓存策略
 * 服务端指定缓存策略，返回相应的response Cache-Control
 * 然而很不幸，大部分的服务端都没有返回Cache-Control来控制缓存，这时客户端就会抛异常：retrofit2.adapter.rxjava2.HttpException: HTTP 504 Unsatisfiable Request (only-if-cached)
 * 所以我们就自己模拟服务端返回一个客户端定义好的缓存策略
 */
internal class NetworkInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val requestCacheControl = request.header("Cache-Control")
        val responseCacheControl = response.header("Cache-Control")
        if (responseCacheControl.isNullOrEmpty() && !requestCacheControl.isNullOrEmpty()) {
            return response.newBuilder().header("Cache-Control", requestCacheControl).build()
        }
        return response
    }

}