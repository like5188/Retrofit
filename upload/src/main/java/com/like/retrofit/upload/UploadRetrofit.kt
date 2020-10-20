package com.like.retrofit.upload

import androidx.lifecycle.MutableLiveData
import com.like.retrofit.RequestConfig
import com.like.retrofit.upload.utils.UploadApi
import com.like.retrofit.util.OkHttpClientFactory
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return this
    }

    /**
     * 上传文件
     *
     * @param url               请求地址。可以是完整路径或者子路径(如果在RequestConfig配置过)
     * @param files             key：上传的文件；value：上传文件的进度监听（Pair<Long, Long>：first为总长度，second为当前上传的进度）
     * @param fileKey           后端确定的File对应的key，后端用它来解析文件。默认："files"
     * @param fileMediaType     上传文件的类型。默认：MediaType.parse("multipart/form-data")
     * @param params            其它参数。默认：null
     * @param paramsMediaType   上传参数的类型。默认：MediaType.parse("text/plain")
     */
    @Throws(UnsupportedOperationException::class)
    suspend fun uploadFiles(
        url: String,
        files: Map<File, MutableLiveData<Pair<Long, Long>>?>,
        fileKey: String = "files",
        fileMediaType: MediaType? = "multipart/form-data".toMediaTypeOrNull(),
        params: Map<String, String>? = null,
        paramsMediaType: MediaType? = "text/plain".toMediaTypeOrNull()
    ): ResponseBody {
        val retrofit = mRetrofit ?: throw UnsupportedOperationException("you must call init() method first")
        val partList: List<MultipartBody.Part> = files.map {
            MultipartBody.Part.createFormData(
                fileKey,
                it.key.name,
                ProgressRequestBody(
                    RequestBody.create(fileMediaType, it.key),
                    it.value
                )
            )
        }
        val par: Map<String, RequestBody> = params?.mapValues {
            it.value.toRequestBody(paramsMediaType)
        } ?: emptyMap()
        return retrofit.create(UploadApi::class.java).uploadFiles(url, partList, par)
    }

    /**
     * 通过progressLiveData返回进度的RequestBody
     * Pair<Long, Long>：first为总长度，second为当前上传的进度
     */
    private class ProgressRequestBody(
        private val delegate: RequestBody,
        private val progressLiveData: MutableLiveData<Pair<Long, Long>>? = null
    ) : RequestBody() {
        private lateinit var bufferedSink: BufferedSink

        override fun contentLength(): Long = delegate.contentLength()

        override fun contentType(): MediaType? = delegate.contentType()

        override fun writeTo(sink: BufferedSink) {
            if (!::bufferedSink.isInitialized) {
                bufferedSink = object : ForwardingSink(sink) {
                    // 总字节数
                    val contentLength = contentLength()

                    // 当前已经上传的字节数
                    var bytesWritten = 0L

                    override fun write(source: Buffer, byteCount: Long) {
                        super.write(source, byteCount)
                        bytesWritten += byteCount
                        progressLiveData?.postValue(Pair(contentLength, bytesWritten))
                    }
                }.buffer()
            }
            delegate.writeTo(bufferedSink)
            // 必须调用flush，否则最后一部分数据可能不会被写入
            bufferedSink.flush()
        }
    }

}