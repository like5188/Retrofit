package com.like.retrofit.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException

/**
 * 自定义的解密拦截器
 */
abstract class DecryptInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) {
            val responseBody = response.body
            if (responseBody == null || response.code == 204) {
                Log.e("DecryptInterceptor", "ResponseBody为null 或者 code=204")
            } else {
                val responseBodyString = responseBody.string()
                if (responseBodyString.isNotEmpty()) {
                    try {
                        val decryptData = decrypt(responseBodyString)
                        if (decryptData.isNotEmpty()) {
                            val newResponseBody = ResponseBody.create(responseBody.contentType(), decryptData)
                            return response.newBuilder().body(newResponseBody).build()
                        }
                    } catch (e: Exception) {
                        // 异常说明解密失败 信息被篡改 直接返回即可
                        Log.e("DecryptInterceptor", "解密异常====》$e")
                    }
                }
            }
        } else {
            Log.e("DecryptInterceptor", "response 返回失败")
        }
        return response
    }

    /**
     * 解密
     *
     * @param data  responseBody.string() 的内容
     */
    abstract fun decrypt(data: String): String
}
