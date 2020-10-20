package com.like.retrofit.download

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.like.retrofit.RequestConfig
import com.like.retrofit.download.factory.GetContentLengthConverterFactory
import com.like.retrofit.download.model.DownloadInfo
import com.like.retrofit.download.utils.DownloadApi
import com.like.retrofit.download.utils.DownloadHelper
import com.like.retrofit.download.utils.merge
import com.like.retrofit.util.OkHttpClientFactory
import com.like.retrofit.util.getCustomNetworkMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import retrofit2.Retrofit
import java.io.File

/**
 * 下载帮助类。
 */
class DownloadRetrofit {
    private var mRetrofit: Retrofit? = null

    fun init(requestConfig: RequestConfig): DownloadRetrofit {
        mRetrofit = Retrofit.Builder()
            .client(OkHttpClientFactory.createOkHttpClientBuilder(requestConfig).build())
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
     * @param threadCount       线程数量，默认为1
     * @param deleteCache       下载之前是否删除已经下载的文件缓存，默认为false
     * @param callbackInterval  数据的发送频率限制，防止下载时发送数据过快，默认200毫秒
     */
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @OptIn(ExperimentalCoroutinesApi::class)
    fun download(
        url: String,
        downloadFile: File,
        threadCount: Int = 1,
        deleteCache: Boolean = false,
        callbackInterval: Long = 200L
    ): Flow<DownloadInfo> {
        // preHandleDownloadInfo 用于实际下载前的一些逻辑处理
        val preHandleDownloadInfo = DownloadInfo().apply {
            this.url = url
            this.downloadFileAbsolutePath = downloadFile.absolutePath
            this.threadCount = threadCount
        }
        var startTime = 0L// 用于 STATUS_RUNNING 状态的发射频率限制，便于更新UI进度。
        var checkParamsResult: CheckParamsResult? = null
        val retrofit = mRetrofit
        return flow {
            if (!checkParamsResult!!.downloaded) {// 没有下载过
                emitAll(DownloadHelper.download(retrofit!!, url, downloadFile, checkParamsResult!!.fileLength, threadCount))
            }
        }.onStart {
            retrofit ?: throw UnsupportedOperationException("you must call init() method first")
            if (deleteCache && !clearCache(downloadFile)) {// 清除缓存失败
                throw RuntimeException("clear cache failed")
            }
            // 如果真正请求前的出现错误，需要单独处理，避免error不能传达到用户。
            checkParamsResult = checkDownloadParams(retrofit, url, downloadFile, threadCount, callbackInterval).apply {
                preHandleDownloadInfo.totalSize = this.fileLength
            }
            startTime = System.currentTimeMillis()
            Log.v("Logger", "开始下载：[${Thread.currentThread().name} ${Thread.currentThread().id}] $preHandleDownloadInfo")
        }.filter {
            // STATUS_RUNNING 状态的发射频率限制
            it.status == DownloadInfo.Status.STATUS_RUNNING && System.currentTimeMillis() - startTime >= callbackInterval
        }.onEach {
            startTime = System.currentTimeMillis()
            Log.d("Logger", "正在下载：[${Thread.currentThread().name} ${Thread.currentThread().id}] $it")
        }.onCompletion { throwable ->
            if (throwable == null) {// 成功完成
                // 合并文件，并删除子文件
                (1..preHandleDownloadInfo.threadCount)
                    .map { File("${preHandleDownloadInfo.downloadFileAbsolutePath}.$it") }
                    .merge(File(preHandleDownloadInfo.downloadFileAbsolutePath), true)
                preHandleDownloadInfo.status = DownloadInfo.Status.STATUS_SUCCESSFUL
                preHandleDownloadInfo.throwable = null
                emit(preHandleDownloadInfo)
                Log.i("Logger", "下载成功：[${Thread.currentThread().name} ${Thread.currentThread().id}] $preHandleDownloadInfo")
            }
        }.catch { throwable ->
            preHandleDownloadInfo.status = DownloadInfo.Status.STATUS_FAILED
            preHandleDownloadInfo.throwable = throwable
            emit(preHandleDownloadInfo)
            Log.e("Logger", "下载失败：[${Thread.currentThread().name} ${Thread.currentThread().id}] ${throwable.getCustomNetworkMessage()}")
        }.flowOn(Dispatchers.IO)

    }

    /**
     * 清除缓存的下载文件
     */
    private fun clearCache(downloadFile: File): Boolean {
        downloadFile.parentFile?.walkTopDown()?.iterator()?.forEach {
            if (it.absolutePath.contains(downloadFile.absolutePath)) {
                if (!it.delete()) {
                    return false
                }
            }
        }
        return true
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