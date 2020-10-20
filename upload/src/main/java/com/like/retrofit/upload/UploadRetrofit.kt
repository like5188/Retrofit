package com.like.retrofit.upload

import androidx.lifecycle.MutableLiveData
import com.like.retrofit.RequestConfig
import com.like.retrofit.upload.utils.ProgressRequestBody
import com.like.retrofit.upload.utils.UploadApi
import com.like.retrofit.util.OkHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.await
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
    @Throws(Exception::class)
    suspend fun uploadFiles(
        url: String,
        files: Map<File, MutableLiveData<Pair<Long, Long>>?>,
        fileKey: String = "files",
        fileMediaType: MediaType? = "multipart/form-data".toMediaTypeOrNull(),
        params: Map<String, String>? = null,
        paramsMediaType: MediaType? = "text/plain".toMediaTypeOrNull()
    ): ResponseBody = withContext(Dispatchers.IO) {
        val retrofit = mRetrofit ?: throw UnsupportedOperationException("you must call init() method first")
        val partList: List<MultipartBody.Part> = files.map {
            MultipartBody.Part.createFormData(
                fileKey,
                it.key.name,
                ProgressRequestBody(it.key.asRequestBody(fileMediaType), it.value)
            )
        }
        val par: Map<String, RequestBody> = params?.mapValues {
            it.value.toRequestBody(paramsMediaType)
        } ?: emptyMap()

        retrofit.create(UploadApi::class.java).uploadFiles(url, partList, par).await()
    }

}