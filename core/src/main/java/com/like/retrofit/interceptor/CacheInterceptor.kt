package com.like.retrofit.interceptor

import android.app.Application
import android.util.Log
import com.like.retrofit.util.NetWorkUtils
import com.like.retrofit.util.TAG
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 自定义的缓存拦截器，需要与[NetworkInterceptor]配合使用
 *
 * 缓存策略为：有网络就从网络获取数据并缓存到本地；无网络就从本地缓存获取数据。
 * 缓存时间在Api的@GET接口中通过@Headers("cache:60")对指定接口设置。
 */
class CacheInterceptor(private val application: Application) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        // 获取Api中的接口设置的：@Headers("cache:60")
        val requestCacheTime = try {
            val cacheTime = request.header("cache")?.toInt() ?: 0// restful api中一定要配合 @headers("cache:60") 使用 针对单独接口进行缓存
            Log.v(TAG, "接口设置了缓存时间：$cacheTime")
            cacheTime
        } catch (e: NumberFormatException) {
            Log.e(TAG, "接口的缓存时间设置格式错误。正确格式：\"cache:60\"")
            0
        } catch (e: Exception) {
            Log.v(TAG, "接口没有设置缓存，默认不缓存。")
            0
        }

        // 如果不缓存
        if (requestCacheTime <= 0) {
            return chain.proceed(request)
        }

        // 接口设置了缓存时间
        if (NetWorkUtils.isConnected(application)) {// 有网时
            Log.i(TAG, "有网络：从网络获取数据并缓存，缓存在$requestCacheTime 秒内可读取")
            return chain.proceed(request)
                .newBuilder()
                .removeHeader("Pragma")// 清除头信息，因为服务器如果不支持，会返回一些干扰信息，不清除下面无法生效
                .header("Cache-Control", "public, max-age=$requestCacheTime")
                .build()
        } else {// 无网时
            // 从缓存中取出，但是要判断缓存是否过期
            val cacheControl = CacheControl.Builder()
                .onlyIfCached()
                .maxStale(requestCacheTime, TimeUnit.SECONDS) //设置缓存的时间，判断缓存是否过期。
                .build()
            request = request.newBuilder()
                .cacheControl(cacheControl)//此处设置了requestCacheTime秒---修改了系统方法，因为系统默认是CacheControl.FORCE_CACHE--是int型最大值
                .build()
            Log.d(TAG, "没有网络：从缓存获取数据")
            return chain.proceed(request)
        }
    }

}