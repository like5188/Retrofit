package com.like.retrofit.util

import com.like.retrofit.RequestConfig
import com.like.retrofit.interceptor.CacheInterceptor
import com.like.retrofit.interceptor.NetworkInterceptor
import okhttp3.Cache
import java.util.concurrent.TimeUnit

object OkHttpClientFactory {
    fun createOkHttpClientBuilder(requestConfig: RequestConfig): okhttp3.OkHttpClient.Builder {
        // 设置日志、超时等基本配置
        val httpClientBuilder = okhttp3.OkHttpClient.Builder()
            .connectTimeout(requestConfig.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(requestConfig.readTimeout, TimeUnit.SECONDS)
            .writeTimeout(requestConfig.writeTimeout, TimeUnit.SECONDS)

        if (requestConfig.getScheme() == "https") {
            // 设置 HTTPS 协议的证书
            when {
                requestConfig.certificateRawResId == -1 -> {
                    // 信任所有证书
                    val sslParams = HttpsUtils.getSSLParams(
                        null,
                        null,
                        null
                    )
                    httpClientBuilder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                }
                requestConfig.certificateRawResId >= 0 -> {
                    // 信任指定证书
                    val sslParams = HttpsUtils.getSSLParams(
                        arrayOf(requestConfig.application.resources.openRawResource(requestConfig.certificateRawResId)),
                        null,
                        null
                    )
                    httpClientBuilder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                }
                else -> {
                    // 信任 Android 系统自带的 CA 证书
                }
            }
            // 设置 HostName 验证
            val hostnameVerifier = if (requestConfig.hostNames.isEmpty()) {
                HttpsUtils.UnSafeHostnameVerifier()
            } else {
                HttpsUtils.SafeHostnameVerifier(requestConfig.hostNames)
            }
            httpClientBuilder.hostnameVerifier(hostnameVerifier)
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