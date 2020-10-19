package com.like.retrofit.download.utils

import android.util.Log
import com.like.retrofit.download.model.DownloadInfo
import kotlinx.coroutines.flow.FlowCollector
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.RandomAccessFile

object DownloadHelper {
    internal suspend fun download(
        flowCollector: FlowCollector<DownloadInfo>,
        retrofit: Retrofit,
        url: String,
        downloadFile: File,
        fileLength: Long,
        threadCount: Int
    ) {
        downloadFile.split(fileLength, threadCount)
            .forEach { splitFileInfo ->
                // 开始下载
                val response = retrofit.create(DownloadApi::class.java)
                    .downloadFile(url, "bytes=${splitFileInfo.from}-${splitFileInfo.to}")
                // 处理返回数据
                val downloadInfo = DownloadInfo().also {
                    it.downloadFileAbsolutePath = splitFileInfo.filePath
                    it.url = url
                    it.threadCount = threadCount
                    it.totalSize = splitFileInfo.totalSize
                }
                handleResponse(flowCollector, downloadInfo, response)
            }
    }

    private suspend fun handleResponse(
        flowCollector: FlowCollector<DownloadInfo>,
        downloadInfo: DownloadInfo,
        response: Response<ResponseBody>
    ) {
        if (response.isSuccessful) {
            val body = response.body()
            if (body == null || response.code() == 204) {// 204 No content，表示请求成功，但没有资源可返回。
                downloadInfo.status = DownloadInfo.Status.STATUS_FAILED
                downloadInfo.throwable = RuntimeException("下载失败：ResponseBody为null 或者 code=204")
            } else {
                // downloadInfo.totalSize <= 0说明range的to比from小。
                if (downloadInfo.cachedSize < downloadInfo.totalSize) {
                    saveBodyToFile(flowCollector, downloadInfo, body)
                }
                downloadInfo.status = DownloadInfo.Status.STATUS_SUCCESSFUL
            }
        } else if (response.code() == 416) {// 416表示请求的range超出范围。就表示已经下载完成了。不知道为什么，416错误有时候不能触发。难道是因为服务端不支持？
            downloadInfo.status = DownloadInfo.Status.STATUS_SUCCESSFUL
        } else {
            downloadInfo.status = DownloadInfo.Status.STATUS_FAILED
            downloadInfo.throwable = RuntimeException("下载失败：code=${response.code()}")
        }
        flowCollector.emit(downloadInfo)
    }

    /**
     * 把 ResponseBody 中的内容存储到 File中
     */
    private suspend fun saveBodyToFile(
        flowCollector: FlowCollector<DownloadInfo>,
        downloadInfo: DownloadInfo,
        body: ResponseBody
    ) = body.byteStream().use { inputStream ->
        RandomAccessFile(File(downloadInfo.downloadFileAbsolutePath), "rwd")
            .apply { seek(downloadInfo.cachedSize) }
            .use { outputStream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead = inputStream.read(buffer)
                while (bytesRead >= 0) {
                    outputStream.write(buffer, 0, bytesRead)

                    downloadInfo.status = DownloadInfo.Status.STATUS_RUNNING
                    flowCollector.emit(downloadInfo)
                    Log.d("Logger", "[${Thread.currentThread().name} ${Thread.currentThread().id}] $downloadInfo")

                    bytesRead = inputStream.read(buffer)
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

}