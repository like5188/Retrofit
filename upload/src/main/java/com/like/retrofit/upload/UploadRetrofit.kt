package com.like.retrofit.upload

import com.like.retrofit.RequestConfig
import com.like.retrofit.upload.utils.ProgressRequestBody
import com.like.retrofit.upload.utils.UploadApi
import com.like.retrofit.util.OkHttpClientFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File

/**
 * 上传帮助类。
 */
class UploadRetrofit {
    private var mRetrofit: Retrofit? = null

    fun init(requestConfig: RequestConfig): UploadRetrofit {
        mRetrofit = Retrofit.Builder()
            .client(
                OkHttpClientFactory.createOkHttpClientBuilder(requestConfig)
                    .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))// 添加日志打印
                    .build()
            )
            .baseUrl(requestConfig.baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())// 处理String和8种基本数据类型的情况。
            .build()
        return this
    }

    /**
     * 上传文件
     *
     * @param url               请求地址。可以是完整路径或者子路径(如果在RequestConfig配置过)
     * @param files             key：上传的文件；value：获取上传文件当前进度的[Flow]
     * @param fileKey           后端确定的File对应的key，后端用它来解析文件。默认："files"
     * @param fileMediaType     上传文件的类型。默认："multipart/form-data"
     * @param params            其它参数。默认：null
     * @param paramsMediaType   上传参数的类型。默认："text/plain"
     * @param callbackInterval  当前进度的发射频率限制，以便更新 UI。默认200毫秒
     */
    @Throws(Exception::class)
    suspend fun uploadFiles(
        url: String?,
        files: Map<File?, ((Flow<Long>) -> Unit)?>,
        fileKey: String = "files",
        fileMediaType: MediaType? = "multipart/form-data".toMediaTypeOrNull(),
        params: Map<String, String>? = null,
        paramsMediaType: MediaType? = "text/plain".toMediaTypeOrNull(),
        callbackInterval: Long = 200L
    ): String {
        val retrofit = mRetrofit ?: throw UnsupportedOperationException("you must call init() method first")
        if (url.isNullOrEmpty()) {
            throw IllegalArgumentException("url is empty")
        }
        if (callbackInterval < 0) {
            throw IllegalArgumentException("callbackInterval is negative")
        }
        val partList: List<MultipartBody.Part> = files.map {
            val file = it.key ?: throw IllegalArgumentException("file is null")
            if (!file.exists()) {
                throw IllegalArgumentException("file is not exists")
            }
            if (file.isDirectory) {
                throw IllegalArgumentException("file is directory")
            }
            val body = ProgressRequestBody(file.asRequestBody(fileMediaType))
            it.value?.invoke(getDataFlow(body, file.length(), callbackInterval))
            MultipartBody.Part.createFormData(fileKey, file.name, body)
        }
        val par: Map<String, RequestBody> = params?.mapValues {
            it.value.toRequestBody(paramsMediaType)
        } ?: emptyMap()
        return retrofit.create(UploadApi::class.java).uploadFiles(url, partList, par)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getDataFlow(progressRequestBody: ProgressRequestBody, fileLength: Long, callbackInterval: Long): Flow<Long> {
        var startTime = 0L// 用于发射频率限制，便于更新UI进度。
        return progressRequestBody.getDataFlow()
            .onStart {
                startTime = System.currentTimeMillis()
            }.filter {
                fileLength == it //保证最后一次一定要发射
                        || System.currentTimeMillis() - startTime >= callbackInterval //发射频率限制
            }.onEach {
                startTime = System.currentTimeMillis()
            }
    }
}