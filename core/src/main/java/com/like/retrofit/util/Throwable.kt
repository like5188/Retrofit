package com.like.retrofit.util

import com.google.gson.JsonParseException
import org.json.JSONException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * 获取自定义的网络请求错误消息，用于显示给用户看。
 */
fun Throwable?.getCustomNetworkMessage() =
    when (this) {
        null -> ""
        is NetworkException -> "没有网络"
        is HttpException -> "网络错误【${this.code()}】"
        is ConnectException -> "网络连接失败"
        is SocketTimeoutException -> "网络连接超时"
        is UnknownHostException, is IOException -> "无法连接到服务器"
        is JsonParseException, is JSONException -> "解析数据错误"
        is SSLHandshakeException -> "证书验证失败"
        else -> this.message
    }