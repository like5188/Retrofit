package com.like.retrofit.download

import android.Manifest
import androidx.annotation.RequiresPermission
import com.like.retrofit.RequestConfig
import com.like.retrofit.download.factory.GetContentLengthConverterFactory
import com.like.retrofit.download.model.DownloadInfo
import com.like.retrofit.download.utils.DownloadApi
import com.like.retrofit.download.utils.DownloadHelper
import com.like.retrofit.download.utils.clearDownloadCaches
import com.like.retrofit.download.utils.merge
import com.like.retrofit.util.OkHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File

/**
 * 下载帮助类。
 */
class DownloadRetrofit {
    private var mRetrofit: Retrofit? = null

    fun init(requestConfig: RequestConfig): DownloadRetrofit {
        mRetrofit = Retrofit.Builder()
            .client(
                OkHttpClientFactory.createOkHttpClientBuilder(requestConfig)
                    .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))// 添加日志打印
                    .build()
            )
            .baseUrl(requestConfig.baseUrl)
            .addConverterFactory(GetContentLengthConverterFactory())// 把返回的ResponseBody转换成long型的contentLength
            .build()
        return this
    }

    /**
     * 下载文件
     *
     * @param url               下载地址。可以是完整路径或者子路径(如果在RequestConfig配置过)
     * @param downloadFile      下载的文件缓存
     * @param threadCount       分成几个子文件进行下载。默认为1，表示不分割。
     * @param deleteCache       下载之前是否删除已经下载的文件缓存，默认为false
     * @param callbackInterval  数据的发送频率限制，防止下载时发送数据过快，默认200毫秒
     */
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @OptIn(ExperimentalCoroutinesApi::class)
    fun downloadFile(
        url: String,
        downloadFile: File,
        threadCount: Int = 1,
        deleteCache: Boolean = false,
        callbackInterval: Long = 200L
    ): Flow<DownloadInfo> {
        // preHandleDownloadInfo 用于实际下载前的一些逻辑处理
        val preHandleDownloadInfo = DownloadInfo().apply {
            this.url = url
            this.absolutePath = downloadFile.absolutePath
            this.threadCount = threadCount
        }
        var startTime = 0L// 用于 STATUS_RUNNING 状态的发射频率限制，便于更新UI进度。
        var checkParamsResult: CheckParamsResult? = null
        val retrofit = mRetrofit

        return flow {
            if (!checkParamsResult!!.downloaded) {// 没有下载过
                emitAll(DownloadHelper.downloadFile(retrofit!!, url, downloadFile, checkParamsResult!!.fileLength, threadCount))
            }
        }.onStart {
            retrofit ?: throw UnsupportedOperationException("you must call init() method first")
            if (deleteCache && !downloadFile.clearDownloadCaches()) {// 清除缓存失败
                throw RuntimeException("clear cache failed")
            }
            // 如果真正请求前的出现错误，需要单独处理，避免error不能传达到用户。
            checkParamsResult = checkDownloadParams(retrofit, url, downloadFile, threadCount, callbackInterval).apply {
                preHandleDownloadInfo.totalSize = this.fileLength
            }
            startTime = System.currentTimeMillis()
        }.filter {
            // STATUS_RUNNING 状态的发射频率限制
            it.status == DownloadInfo.Status.STATUS_RUNNING && System.currentTimeMillis() - startTime >= callbackInterval
        }.map {
            if (threadCount > 1 && it.status == DownloadInfo.Status.STATUS_RUNNING) {
                // 多协程下载。合并 STATUS_RUNNING 数据，以便正确显示 totalSize 和 cachedSize，用于UI的进度显示
                preHandleDownloadInfo.apply {
                    this.status = it.status
                    this.throwable = it.throwable
                }
            } else {
                it
            }
        }.onEach {
            startTime = System.currentTimeMillis()
        }.onCompletion { throwable ->
            if (throwable == null) {// 成功完成
                if (threadCount > 1) {// 多协程下载。合并文件，并删除子文件
                    (1..preHandleDownloadInfo.threadCount)
                        .map { File("${preHandleDownloadInfo.absolutePath}.$it") }
                        .merge(File(preHandleDownloadInfo.absolutePath), true)
                }
                preHandleDownloadInfo.status = DownloadInfo.Status.STATUS_SUCCESS
                preHandleDownloadInfo.throwable = null
                emit(preHandleDownloadInfo)
            }
        }.catch { throwable ->
            preHandleDownloadInfo.status = DownloadInfo.Status.STATUS_FAILED
            preHandleDownloadInfo.throwable = throwable
            emit(preHandleDownloadInfo)
        }.flowOn(Dispatchers.IO)

    }

    private suspend fun checkDownloadParams(
        retrofit: Retrofit,
        url: String,
        downloadFile: File,
        threadCount: Int,
        callbackInterval: Long
    ): CheckParamsResult = CheckParamsResult().also {
        val checkParamsException = when {
            url.isEmpty() -> IllegalArgumentException("url isEmpty")
            downloadFile.isDirectory -> IllegalArgumentException("downloadFile isDirectory")
            threadCount < 1 -> IllegalArgumentException("threadCount must be greater than or equal to 1")
            callbackInterval <= 0 -> IllegalArgumentException("callbackInterval must be greater than 0")
            else -> null
        }
        if (checkParamsException != null) {
            throw checkParamsException
        }
        it.fileLength = retrofit.create(DownloadApi::class.java).getContentLength(url)
        if (it.fileLength <= 0L) {
            throw IllegalArgumentException("getContentLength from net failure")
        } else if (downloadFile.exists() && downloadFile.length() == it.fileLength) {
            it.downloaded = true
        }
    }

    private class CheckParamsResult {
        var fileLength = 0L
        var downloaded = false
    }
}