package com.like.retrofit.sample

import android.app.Application
import com.like.retrofit.RequestConfig
import com.like.retrofit.common.CommonRetrofit
import com.like.retrofit.download.DownloadRetrofit
import com.like.retrofit.interceptor.DecryptInterceptor
import com.like.retrofit.interceptor.EncryptInterceptor
import com.like.retrofit.interceptor.HeaderInterceptor
import com.like.retrofit.interceptor.PublicParamsInterceptor
import com.like.retrofit.upload.UploadRetrofit
import com.like.retrofit.util.aesDecrypt
import com.like.retrofit.util.aesEncrypt

class MyApplication : Application() {
    companion object {
        val mCommonRetrofit = CommonRetrofit()
        val mDownloadRetrofit = DownloadRetrofit()
        val mUploadRetrofit = UploadRetrofit()
    }

    override fun onCreate() {
        super.onCreate()
        val publicParamsInterceptor = object : PublicParamsInterceptor() {
            override fun getPublicParamsMap(): Map<String, String> {
                return mapOf("PublicParams" to "PublicParams")
            }
        }
        val headerInterceptor = object : HeaderInterceptor() {
            override fun getHeaderMap(): Map<String, String> {
                return mapOf("header" to "header")
            }
        }
        val encryptInterceptor = object : EncryptInterceptor() {
            override fun encryptUrlParams(data: String): String {
                return "params=${data.aesEncrypt("1234567890123456")}"
            }

            override fun encryptBodyParams(data: String): String {
                return data.aesEncrypt("1234567890123456")
            }
        }
        val decryptInterceptor = object : DecryptInterceptor() {
            override fun decrypt(data: String): String {
                return data.aesDecrypt("1234567890123456")
            }
        }

        // hlwyyssl.jkscw.com.cn 测试 postBody 方法。
        mCommonRetrofit.init(
            RequestConfig(
                application = this@MyApplication,
                baseUrl = "http://61.186.170.66:8800/xxc/",
                interceptors = listOf(publicParamsInterceptor, headerInterceptor)
            )
        )

        mDownloadRetrofit.init(RequestConfig(this@MyApplication))

        mUploadRetrofit.init(RequestConfig(this@MyApplication))
    }

}