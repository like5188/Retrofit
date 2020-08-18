package com.like.retrofit.interceptor

import android.util.Log
import com.like.retrofit.utils.string
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

/**
 * 自定义的加密拦截器
 */
abstract class EncryptInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val oldRequest = chain.request()
        when (oldRequest.method) {
            "GET", "DELETE" -> {// 请求数据是拼接在请求地址后面的
                val url = oldRequest.url
                // 本次请求的接口地址
                val apiPath = "${url.scheme}://${url.host}:${url.port}${url.encodedPath}".trim()
                // 如果有请求参数，则加密?号后面的所有参数。
                val queryParams = url.encodedQuery
                if (!queryParams.isNullOrEmpty()) {
                    try {
                        //拼接加密后的url
                        val newUrl = "$apiPath?${encryptUrlParams(queryParams)}"
                        //构建新的请求
                        val newRequest = oldRequest.newBuilder().url(newUrl).build()
                        return chain.proceed(newRequest)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            "POST", "PUT" -> {// 请求数据在请求体中
                val requestBody = oldRequest.body
                if (requestBody != null) {
                    val requestBodyString = requestBody.string()
                    if (requestBodyString.isNotEmpty()) {
                        try {
                            val encryptData = encryptBodyParams(requestBodyString)
                            if (encryptData.isNotEmpty()) {
                                val newRequestBody = RequestBody.create(requestBody.contentType(), encryptData)
                                // 根据请求方式构建相应的请求
                                if ("POST" == oldRequest.method) {
                                    val newRequest = oldRequest.newBuilder().post(newRequestBody).build()
                                    return chain.proceed(newRequest)
                                } else if ("PUT" == oldRequest.method) {
                                    val newRequest = oldRequest.newBuilder().put(newRequestBody).build()
                                    return chain.proceed(newRequest)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("EncryptInterceptor", "加密异常====》$e")
                        }
                    }
                }
            }
        }
        return chain.proceed(oldRequest)
    }

    /**
     * 加密Url中的参数。比如：params=xxxxxx，其中params是和后台商量后得出的key
     *
     * @param data  ?后面的所有字符串。
     */
    abstract fun encryptUrlParams(data: String): String

    /**
     * 加密请求体中的参数。
     *
     * @param data  responseBody.string() 的内容
     */
    abstract fun encryptBodyParams(data: String): String

}
