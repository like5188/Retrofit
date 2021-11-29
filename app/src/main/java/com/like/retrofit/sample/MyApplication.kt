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
                return mapOf(
                    "X-Access-Token" to "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE2MDM1MDY1NzgsInVzZXJuYW1lIjoidGVzdDMxIn0.G8A6VENVhYX0eV6GGZ7gsSJx2Hq5tUxFOxAKdNvOxQo"
                )
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

        mCommonRetrofit.init(
            RequestConfig(
                application = this@MyApplication,
                baseUrl = "https://www.wanandroid.com/",
//                certificateRawResId = -2,
                hostNames = listOf("www.wanandroid.com"),
                interceptors = listOf(publicParamsInterceptor, headerInterceptor)
            )
        )

        mDownloadRetrofit.init(
            RequestConfig(
                application = this@MyApplication,
                interceptors = listOf(headerInterceptor)
            )
        )

        mUploadRetrofit.init(
            RequestConfig(
                application = this@MyApplication,
                interceptors = listOf(headerInterceptor)
            )
        )
    }

}