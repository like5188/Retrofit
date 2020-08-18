package com.like.retrofit.utils

import com.like.retrofit.RequestConfig
import com.like.retrofit.interceptor.CacheInterceptor
import com.like.retrofit.interceptor.NetworkInterceptor
import okhttp3.Cache
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier

object OkHttpClientFactory {
    fun createOkHttpClientBuilder(requestConfig: RequestConfig): okhttp3.OkHttpClient.Builder {
        // 设置日志、超时等基本配置
        val httpClientBuilder = okhttp3.OkHttpClient.Builder()
            .connectTimeout(requestConfig.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(requestConfig.readTimeout, TimeUnit.SECONDS)
            .writeTimeout(requestConfig.writeTimeout, TimeUnit.SECONDS)

        // 设置HTTPS协议的证书
        if (requestConfig.getScheme() == "https" && requestConfig.certificateRawResId != -1) {
            HttpsUtil.getSslSocketFactory(
                arrayOf(requestConfig.application.resources.openRawResource(requestConfig.certificateRawResId)),
                null, null
            )?.let {
                // 接下来给okhttp绑定证书
                httpClientBuilder.sslSocketFactory(it.sSLSocketFactory, it.trustManager)
                    .hostnameVerifier(HostnameVerifier { hostname, session ->
                        // 强行返回true，忽略HostName验证 即验证成功
                        true
                    })
            }
        }

        // 设置拦截器
        requestConfig.interceptors?.forEach {
            if (it is CacheInterceptor) {// 如果设置了缓存拦截器，就设置缓存。
                // 10M 缓存。
                httpClientBuilder.cache(Cache(requestConfig.application.cacheDir, (10 * 1024 * 1024).toLong()))
                    .addNetworkInterceptor(NetworkInterceptor())// 必须添加 NetworkInterceptor。避免504错误
            }
            httpClientBuilder.addInterceptor(it)
        }
        return httpClientBuilder
    }
}