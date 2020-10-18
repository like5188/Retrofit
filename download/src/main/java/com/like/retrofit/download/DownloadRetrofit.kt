package com.like.retrofit.download

import com.like.retrofit.download.factory.GetContentLengthConverterFactory
import com.like.retrofit.download.livedata.CoroutineDownloadLiveData
import com.like.retrofit.download.livedata.CoroutineDownloadLiveDataManager
import com.like.retrofit.download.livedata.DownloadLiveData
import com.like.retrofit.util.OkHttpClientFactory
import com.like.retrofit.RequestConfig
import com.like.retrofit.download.model.DownloadInfo
import com.like.retrofit.download.utils.DownloadApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    @Throws(UnsupportedOperationException::class)
    suspend fun download(
        url: String,
        downloadFile: File,
        threadCount: Int = 1,
        deleteCache: Boolean = false,
        callbackInterval: Long = 200L
    ): DownloadLiveData {
        val retrofit =
            mRetrofit ?: throw UnsupportedOperationException("you must call init() method first")
        // preHandleDownloadInfo 用于实际下载前的一些逻辑处理
        val preHandleDownloadInfo = DownloadInfo()
            .apply {
            this.url = url
            this.downloadFileAbsolutePath = downloadFile.absolutePath
            this.threadCount = threadCount
        }

        if (deleteCache && !clearCache(downloadFile)) {// 清除缓存失败
            preHandleDownloadInfo.status = DownloadInfo.Status.STATUS_FAILED
            preHandleDownloadInfo.throwable = RuntimeException("clear cache failed")
            return CoroutineDownloadLiveData(
                preHandleDownloadInfo
            )
        }

        // 如果真正请求前的出现错误，需要单独处理，避免error不能传达到用户。
        val checkParamsResult =
            checkDownloadParamsAsync(retrofit, url, downloadFile, threadCount, callbackInterval)
        return when {
            checkParamsResult.downloaded -> {// 已经下载过
                preHandleDownloadInfo.totalSize = downloadFile.length()
                preHandleDownloadInfo.status = DownloadInfo.Status.STATUS_SUCCESSFUL
                preHandleDownloadInfo.throwable = null
                CoroutineDownloadLiveData(
                    preHandleDownloadInfo
                )
            }
            checkParamsResult.exception != null -> {// 参数有误
                preHandleDownloadInfo.status = DownloadInfo.Status.STATUS_FAILED
                preHandleDownloadInfo.throwable = checkParamsResult.exception
                CoroutineDownloadLiveData(
                    preHandleDownloadInfo
                )
            }
            else -> {
                preHandleDownloadInfo.totalSize = checkParamsResult.fileLength
                val coroutineCallLiveDataManager =
                    CoroutineDownloadLiveDataManager(
                        preHandleDownloadInfo,
                        callbackInterval
                    )

                downloadFile.split(checkParamsResult.fileLength, threadCount)
                    .forEach { splitFileInfo ->
                        val downloadInfo = DownloadInfo()
                            .also {
                            it.downloadFileAbsolutePath = splitFileInfo.filePath
                            it.url = url
                            it.threadCount = threadCount
                            it.totalSize = splitFileInfo.totalSize
                        }
                        // 开始下载
                        val call = retrofit.create(DownloadApi::class.java)
                            .downloadFile(
                                downloadInfo.url,
                                "bytes=${splitFileInfo.from}-${splitFileInfo.to}"
                            )
                        coroutineCallLiveDataManager.addLiveData(
                            CoroutineDownloadLiveData(
                                downloadInfo,
                                call
                            )
                        )
                    }

                if (coroutineCallLiveDataManager.hasLiveData()) {
                    coroutineCallLiveDataManager
                } else {
                    preHandleDownloadInfo.status = DownloadInfo.Status.STATUS_FAILED
                    preHandleDownloadInfo.throwable =
                        IllegalArgumentException("create retrofit download service failed")
                    CoroutineDownloadLiveData(
                        preHandleDownloadInfo
                    )
                }
            }
        }
    }

    private suspend fun checkDownloadParamsAsync(
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
            it.exception = checkParamsException
            return@also
        }
        withContext(Dispatchers.IO) {
            try {
                it.fileLength = retrofit.create(DownloadApi::class.java).getContentLength(url)
                if (it.fileLength <= 0L) {
                    it.exception = IllegalArgumentException("getContentLength from net failure")
                } else if (downloadFile.exists() && downloadFile.length() == it.fileLength) {
                    it.downloaded = true
                }
            } catch (e: Exception) {
                it.exception = e
            }
        }
    }

    /**
     * 根据 threadCount 来分割文件进行下载
     */
    private fun File.split(
        fileLength: Long,
        threadCount: Int
    ): List<SplitFileInfo> {
        val result = mutableListOf<SplitFileInfo>()
        // Range:(unit=first byte pos)-[last byte pos]，其中 first byte pos 从0开始
        when {
            threadCount == 1 -> {
                result.add(
                    SplitFileInfo(
                        this,
                        0L,
                        fileLength - 1
                    )
                )
            }
            threadCount > 1 -> {
                // 每个子文件的大小。最后一个子文件的大小<=blockSize
                val blockSize = if (fileLength % threadCount == 0L) {
                    fileLength / threadCount
                } else {
                    fileLength / threadCount + 1
                }
                for (i in (1..threadCount)) {
                    result.add(
                        SplitFileInfo(
                            File("${this.absolutePath}.$i"),
                            blockSize * (i - 1),
                            if (i == threadCount) {
                                fileLength - 1
                            } else {
                                blockSize * i.toLong() - 1
                            }
                        )
                    )
                }
            }
        }
        return result
    }

    /**
     * 清除缓存的下载文件
     */
    private fun clearCache(downloadFile: File): Boolean {
        try {
            downloadFile.parentFile?.walkTopDown()?.iterator()?.forEach {
                if (it.absolutePath.contains(downloadFile.absolutePath)) {
                    if (!it.delete()) {
                        return false
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 按线程数分割的文件信息
     *
     * @param file  分割的文件
     * @param first 本文件数据在分割前文件的起点位置
     * @param to    本次下载的终点位置
     */
    private class SplitFileInfo(private val file: File, private val first: Long, val to: Long) {
        /**
         * 本文件的路径
         */
        val filePath = file.absolutePath ?: ""

        /**
         * 本文件已经缓存的大小
         */
        private val cachedSize = if (file.exists()) {
            file.length()
        } else {
            0
        }

        /**
         * 本次下载的起点位置
         */
        val from = first + cachedSize

        /**
         * 本文件的总大小
         */
        val totalSize = to - first + 1

        override fun toString(): String {
            return "SplitFileInfo(filePath='$filePath', first=$first, cachedSize=$cachedSize, from=$from, to=$to, totalSize=$totalSize)"
        }

    }

    private class CheckParamsResult {
        var fileLength = 0L
        var downloaded = false
        var exception: Exception? = null
    }

}