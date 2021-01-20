package com.like.retrofit.upload

import android.Manifest
import androidx.annotation.RequiresPermission
import com.like.retrofit.RequestConfig
import com.like.retrofit.upload.model.UploadInfo
import com.like.retrofit.upload.utils.ProgressRequestBody
import com.like.retrofit.upload.utils.UploadApi
import com.like.retrofit.util.OkHttpClientFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
     * @param file              上传的文件
     * @param fileKey           后端确定的File对应的key，后端用它来解析文件。默认："file"
     * @param fileMediaType     上传文件的类型。默认：MediaType.parse("multipart/form-data")
     * @param params            其它参数。默认：null
     * @param paramsMediaType   上传参数的类型。默认：MediaType.parse("text/plain")
     * @param callbackInterval  数据的发送频率限制，防止发送数据过快。默认200毫秒
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    @Throws(Exception::class)
    fun uploadFiles(
        coroutineScope: CoroutineScope,
        url: String,
        file: File,
        fileKey: String = "file",
        fileMediaType: MediaType? = "multipart/form-data".toMediaTypeOrNull(),
        params: Map<String, String>? = null,
        paramsMediaType: MediaType? = "text/plain".toMediaTypeOrNull(),
        callbackInterval: Long = 200L
    ): Flow<UploadInfo> {
        // preHandleUploadInfo 用于实际上传前的一些逻辑处理
        val preHandleUploadInfo = UploadInfo().apply {
            this.url = url
            this.totalSize = file.length()
            this.absolutePath = file.absolutePath
        }
        var startTime = 0L// 用于 STATUS_RUNNING 状态的发射频率限制，便于更新UI进度。
        val retrofit = mRetrofit

        val stateFlow = MutableStateFlow(preHandleUploadInfo)
        coroutineScope.launch {
            try {
                retrofit ?: throw UnsupportedOperationException("you must call init() method first")
                // 如果真正请求前的出现错误，需要单独处理，避免error不能传达到用户。
                checkUploadParams(url, file, callbackInterval)
                val body = ProgressRequestBody(stateFlow, url, file, file.asRequestBody(fileMediaType))
                val part = MultipartBody.Part.createFormData(fileKey, file.name, body)
                val par: Map<String, RequestBody> = params?.mapValues {
                    it.value.toRequestBody(paramsMediaType)
                } ?: emptyMap()
                retrofit.create(UploadApi::class.java).uploadFiles(url, part, par)
            } catch (e: Exception) {
                preHandleUploadInfo.status = UploadInfo.Status.STATUS_FAILED
                preHandleUploadInfo.throwable = e
                stateFlow.value = preHandleUploadInfo
            }
        }
        return stateFlow.onStart {
            startTime = System.currentTimeMillis()
        }.filter {
            // STATUS_RUNNING 状态的发射频率限制，这里不能像下载那样判断，因为上面coroutineScope.launch代码块里面的异常不会触发catch代码块。
            if (it.status != UploadInfo.Status.STATUS_RUNNING) {
                true
            } else {
                it.totalSize == it.uploadSize || System.currentTimeMillis() - startTime >= callbackInterval
            }
        }.onEach {
            startTime = System.currentTimeMillis()
        }.onCompletion { throwable ->
            if (throwable == null) {// 成功完成
                preHandleUploadInfo.status = UploadInfo.Status.STATUS_SUCCESS
                preHandleUploadInfo.throwable = null
                emit(preHandleUploadInfo)
            }
        }.catch { throwable ->
            preHandleUploadInfo.status = UploadInfo.Status.STATUS_FAILED
            preHandleUploadInfo.throwable = throwable
            emit(preHandleUploadInfo)
        }.flowOn(Dispatchers.IO)
    }

    @Throws(IllegalArgumentException::class)
    private fun checkUploadParams(
        url: String,
        file: File,
        callbackInterval: Long
    ) {
        val checkParamsException = when {
            url.isEmpty() -> IllegalArgumentException("url isEmpty")
            file.isDirectory -> IllegalArgumentException("downloadFile isDirectory")
            callbackInterval <= 0 -> IllegalArgumentException("callbackInterval must be greater than 0")
            else -> null
        }
        if (checkParamsException != null) {
            throw checkParamsException
        }
    }

}