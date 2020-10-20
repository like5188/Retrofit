package com.like.retrofit.download.utils

import com.like.retrofit.download.model.DownloadInfo
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.io.File
import java.io.RandomAccessFile

object DownloadHelper {
    @OptIn(FlowPreview::class)
    internal fun download(
        retrofit: Retrofit,
        url: String,
        downloadFile: File,
        fileLength: Long,
        threadCount: Int
    ): Flow<DownloadInfo> {
        val splitFileInfos = downloadFile.preSplit(fileLength, threadCount)
        return if (threadCount == 1) {
            download(splitFileInfos[0], retrofit, url, threadCount)
        } else {
            splitFileInfos.asFlow().flatMapMerge {
                download(it, retrofit, url, threadCount)
            }
        }
    }

    private fun download(
        splitFileInfo: SplitFileInfo,
        retrofit: Retrofit,
        url: String,
        threadCount: Int
    ) = flow {
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
        if (response.isSuccessful) {
            val body = response.body()
            if (body == null || response.code() == 204) {// 204 No content，表示请求成功，但没有资源可返回。
                throw RuntimeException("ResponseBody为null 或者 code=204")
            } else {
                // downloadInfo.totalSize <= 0说明range的to比from小。
                if (downloadInfo.cachedSize < downloadInfo.totalSize) {
                    saveBodyToFile(this, downloadInfo, body)
                }
            }
        } else if (response.code() != 416) {// 416表示请求的range超出范围。就表示已经下载完成了。不知道为什么，416错误有时候不能触发。难道是因为服务端不支持？
            throw RuntimeException("code=${response.code()}")
        }
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

                    bytesRead = inputStream.read(buffer)
                }
            }
    }

}