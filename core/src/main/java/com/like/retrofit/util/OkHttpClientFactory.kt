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

        // 设置 HTTPS 协议的证书、HostName 验证
        if (requestConfig.getScheme() == "https") {
            val certificates = if (requestConfig.certificateRawResId == -1) {
                null
            } else {
                arrayOf(requestConfig.application.resources.openRawResource(requestConfig.certificateRawResId))
            }

            val hostnameVerifier = if (requestConfig.hostNames.isEmpty()) {
                HttpsUtils.UnSafeHostnameVerifier()
            } else {
                HttpsUtils.SafeHostnameVerifier(requestConfig.hostNames)
            }

            HttpsUtils.getSSLParams(certificates, null, null)?.apply {
                httpClientBuilder.sslSocketFactory(sSLSocketFactory, trustManager).hostnameVerifier(hostnameVerifier)
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