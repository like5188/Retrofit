@file:Suppress("BlockingMethodInNonBlockingContext")

package com.like.retrofit.download.utils

import com.like.retrofit.download.model.DownloadInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.File
import java.io.RandomAccessFile

object DownloadHelper {
    @OptIn(FlowPreview::class)
    internal fun downloadFile(
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
            .downloadFile(url, splitFileInfo.getRangeHeader())

        // 处理返回数据
        if (response.isSuccessful) {
            if (response.code() == 204) {// 204 No content，表示请求成功，但没有资源可返回。
                throw RuntimeException("response code is 204")
            }
            val responseBody = response.body() ?: throw RuntimeException("responseBody is null")
            val downloadInfo = DownloadInfo().also {
                it.url = url
                it.threadCount = threadCount
                it.absolutePath = splitFileInfo.filePath
                it.totalSize = splitFileInfo.totalSize
            }

            withContext(Dispatchers.IO) {
                // 把 ResponseBody 中的内容存储到 File中
                responseBody.byteStream().use { inputStream ->
                    RandomAccessFile(File(downloadInfo.absolutePath), "rwd")
                        .apply { seek(downloadInfo.cachedSize) }
                        .use { outputStream ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var bytesRead = inputStream.read(buffer)
                            while (bytesRead >= 0) {
                                outputStream.write(buffer, 0, bytesRead)

                                downloadInfo.status = DownloadInfo.Status.STATUS_RUNNING
                                emit(downloadInfo)

                                bytesRead = inputStream.read(buffer)
                            }
                        }
                }
            }
        } else if (response.code() != 416) {// 416表示请求的range超出范围。就表示已经下载完成了。不知道为什么，416错误有时候不能触发。难道是因为服务端不支持？
            throw HttpException(response)
        }
    }

}