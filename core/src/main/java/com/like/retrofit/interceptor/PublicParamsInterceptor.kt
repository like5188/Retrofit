package com.like.retrofit.interceptor

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.like.retrofit.util.TAG
import com.like.retrofit.util.string
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * 自定义的添加公共参数的拦截器，支持 GET、POST 请求
 */
abstract class PublicParamsInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val publicParamsMap = getPublicParamsMap()
        val oldRequest = chain.request()
        val newRequest = if (publicParamsMap.isNullOrEmpty()) {
            oldRequest
        } else {
            // 添加公共参数
            when (oldRequest.method) {
                "GET" -> addGetParams(oldRequest, publicParamsMap)
                "POST" -> addPostParams(oldRequest, publicParamsMap)
                else -> oldRequest
            }
        }
        return chain.proceed(newRequest)
    }

    private fun addGetParams(oldRequest: Request, publicParamsMap: Map<String, String>): Request {
        val newRequestUrlBuilder = oldRequest.url.newBuilder()
        publicParamsMap.entries.forEach {
            newRequestUrlBuilder.addQueryParameter(it.key, it.value)
        }
        val newUrl = newRequestUrlBuilder.build()
        Log.i(TAG, "PublicParamsInterceptor GET newUrl：$newUrl")
        return oldRequest.newBuilder().url(newUrl).build()
    }

    private fun addPostParams(oldRequest: Request, publicParamsMap: Map<String, String>): Request =
        when (oldRequest.body) {
            is FormBody -> {// API中使用@Field、@FieldMap时。
                addPostParamsToFormBody(oldRequest, publicParamsMap)
            }
            is MultipartBody -> {// API中使用@Part、@PartMap时。
                addPostParamsToMultipartBody(oldRequest, publicParamsMap)
            }
            is RequestBody -> {// API中使用@Body时
                addPostParamsToRequestBody(oldRequest, publicParamsMap)
            }
            else -> {
                oldRequest
            }
        }

    /**
     * API中使用@Field、@FieldMap时，添加公共参数到请求体中
     * 参数样式：PublicParams=likePublicParams&username=like5188&password=like5488
     *
     * @param oldRequest        当前请求
     * @param publicParamsMap   需要添加的公共参数
     * @return 添加了公共参数后的请求
     */
    private fun addPostParamsToFormBody(oldRequest: Request, publicParamsMap: Map<String, String>): Request {
        // 获取公共参数字符串
        val formBodyBuilder = FormBody.Builder()
        publicParamsMap.entries.forEach {
            formBodyBuilder.add(it.key, it.value)
        }
        val formBody = formBodyBuilder.build()
        val newRequestBodyString = formBody.string()
        // 获取传递的参数的字符串
        val oldRequestBodyString = oldRequest.body.string()
        // 组合公共参数和传递的参数
        val requestBodyString = if (oldRequestBodyString.isNotEmpty()) {
            "$newRequestBodyString&$oldRequestBodyString"
        } else {
            newRequestBodyString
        }
        return oldRequest.newBuilder().post(
            requestBodyString.toRequestBody(
                "application/x-www-form-urlencoded;charset=UTF-8".toMediaTypeOrNull()
            )
        ).build()
    }

    /**
     * API中使用@Part、@PartMap时，添加公共参数到请求体中
     * 参数样式：
     * D/OkHttp: --209da622-5992-4430-a450-5234bc77226b
     * D/OkHttp: Content-Disposition: form-data; name="PublicParams"
     * D/OkHttp: Content-Length: 16
     * D/OkHttp: likePublicParams
     *  D/OkHttp: --209da622-5992-4430-a450-5234bc77226b
     * D/OkHttp: Content-Disposition: form-data; name="username"
     * D/OkHttp: Content-Transfer-Encoding: binary
     * D/OkHttp: Content-Type: text/plain; charset=UTF-8
     * D/OkHttp: Content-Length: 8
     * D/OkHttp: like5188
     * D/OkHttp: --209da622-5992-4430-a450-5234bc77226b
     * D/OkHttp: Content-Disposition: form-data; name="password"
     * D/OkHttp: Content-Transfer-Encoding: binary
     * D/OkHttp: Content-Type: text/plain; charset=UTF-8
     * D/OkHttp: Content-Length: 8
     * D/OkHttp: like5488
     * D/OkHttp: --209da622-5992-4430-a450-5234bc77226b--
     *
     * 注意：如果这里不加 addConverterFactory(ScalarsConverterFactory.create()) 的话，
     * D/OkHttp: like5188 会变成 D/OkHttp: "like5188"
     * 因为@Part、@PartMap持的数据类型如果是其它数据类型，则需要 retrofit2.Converter 进行转换，
     * 这里的ScalarsConverterFactory 支持基本数据类型转换为"text/plain"类型的RequestBody
     *
     * @param oldRequest        当前请求
     * @param publicParamsMap   需要添加的公共参数
     * @return 添加了公共参数后的请求
     */
    private fun addPostParamsToMultipartBody(oldRequest: Request, publicParamsMap: Map<String, String>): Request {
        val multipartBodyBuilder = MultipartBody.Builder()
        multipartBodyBuilder.setType(MultipartBody.FORM)
        // 添加公共参数
        publicParamsMap.entries.forEach {
            multipartBodyBuilder.addFormDataPart(it.key, it.value)
        }
        // 添加传递的参数
        (oldRequest.body as MultipartBody).parts.forEach {
            multipartBodyBuilder.addPart(it)
        }
        return oldRequest.newBuilder().post(multipartBodyBuilder.build()).build()
    }

    /**
     * API中使用@Body时，添加公共参数到请求体中
     * 参数样式：{"PublicParams":"2","username":"13508129810","password":"123456"}
     * 注意：这里只处理 contentType 为 “application/json; charset=UTF-8” 的情况。
     *
     * @param oldRequest        当前请求
     * @param publicParamsMap   需要添加的公共参数
     * @return 添加了公共参数后的请求
     */
    private fun addPostParamsToRequestBody(oldRequest: Request, publicParamsMap: Map<String, String>): Request {
        val oldRequestBody = oldRequest.body
        val contentType = oldRequestBody?.contentType()
        // 注意：这里 UTF-8 区分大小写
        return if (contentType == "application/json; charset=UTF-8".toMediaTypeOrNull()) {
            val newRequestParams = JsonObject()
            // 添加公共参数
            publicParamsMap.entries.forEach {
                newRequestParams.addProperty(it.key, it.value)
            }
            // 添加传递的参数
            val oldRequestBodyString = oldRequestBody.string()
            val oldRequestParams = try {
                JsonParser().parse(oldRequestBodyString).asJsonObject
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            oldRequestParams?.entrySet()?.forEach {
                newRequestParams.add(it.key, it.value)
            }

            oldRequest.newBuilder().post(newRequestParams.toString().toRequestBody(contentType)).build()
        } else {// 比如："text/plain; charset=UTF-8"
            oldRequest
        }
    }

    abstract fun getPublicParamsMap(): Map<String, String>

}
