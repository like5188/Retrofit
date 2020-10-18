package com.like.retrofit

import android.app.Application
import android.widget.Toast
import okhttp3.Interceptor

/**
 * 网络请求相关配置
 *
 * @param application
 * @param baseUrl               默认"https://www.xxx.com/"。正确格式为："http（https）:"开头，"/"结尾
 * @param connectTimeout        连接超时时间（单位秒）
 * @param readTimeout           读超时时间（单位秒）
 * @param writeTimeout          写超时时间（单位秒）
 * @param certificateRawResId   当采用RetrofitUtils.SCHEME_HTTPS请求时，证书文件的资源id。证书文件必须放在res/raw/目录下
 * @param interceptors          自定义的拦截器，继承自[Interceptor]。
 * [com.like.retrofit.interceptor]中提供了几个默认拦截器：
 * [com.like.retrofit.interceptor.CacheInterceptor]、
 * [com.like.retrofit.interceptor.HeaderInterceptor]、
 * [com.like.retrofit.interceptor.PublicParamsInterceptor]、
 * [com.like.retrofit.interceptor.EncryptInterceptor]、
 * [com.like.retrofit.interceptor.DecryptInterceptor]
 */
data class RequestConfig(
    val application: Application,
    val baseUrl: String = DEFAULT_URL,
    val connectTimeout: Long = 5L,
    val readTimeout: Long = 10L,
    val writeTimeout: Long = 60L,
    val certificateRawResId: Int = -1,
    val interceptors: List<Interceptor>? = null
) {
    companion object {
        // 因为接口可以传入@Url或者直接使用完整url（retrofit框架会判断，在使用包含协议的完整url时，就直接使用它，而不使用baseUrl），
        // 所以外部可以不传baseUrl参数，但是Retrofit又必须要baseUrl，所以这里写一个假的。
        const val DEFAULT_URL = "https://www.xxx.com/"

        /**
         * 正确格式为："http（https）:"开头，"/"结尾
         */
        fun isUrlValid(url: String): Boolean {
            if (!url.endsWith("/")) {
                return false
            }
            if (!url.startsWith("http:") && !url.startsWith("https:")) {
                return false
            }
            return true
        }
    }

    init {
        if (!isUrlValid(baseUrl)) {
            Toast.makeText(
                application,
                "baseUrl must end in / and Expected URL scheme 'http' or 'https' but no colon was found",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 获取协议："https"或者"http"
     */
    fun getScheme() = if (baseUrl.contains("https")) {
        "https"
    } else {
        "http"
    }

}

