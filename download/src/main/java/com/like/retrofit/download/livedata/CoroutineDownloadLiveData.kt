package com.like.retrofit.download.livedata

import com.like.retrofit.download.model.DownloadInfo
import com.like.retrofit.download.model.DownloadInfo.Status.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 利用协程进行下载，并把结果通过 postValue() 传递出去
 */
class CoroutineDownloadLiveData(
    private val downloadInfo: DownloadInfo,
    private val call: Call<ResponseBody>? = null
) : DownloadLiveData(call) {
    private var started = AtomicBoolean(false)

    override fun onActive() {
        super.onActive()
        if (started.compareAndSet(false, true)) {
            if (call == null) {
                postValue(downloadInfo)
            } else {
                // 这里不能用外面传递进来的 CoroutineScope，因为要在多个独立的协程里面运行，
                // 还有就是在退出应用程序后也需要下载。
                GlobalScope.launch(Dispatchers.IO) {
                    download(call, downloadInfo)
                }
            }
        }
    }

    /**
     * 执行 Call 开始下载
     */
    private fun download(call: Call<ResponseBody>, downloadInfo: DownloadInfo) {
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()
                if (body == null || response.code() == 204) {// 204 No content，表示请求成功，但没有资源可返回。
                    downloadInfo.updateStatusAndThrowable(
                        STATUS_FAILED,
                        RuntimeException("下载失败：ResponseBody为null 或者 code=204")
                    )
                } else {
                    // downloadInfo.totalSize <= 0说明range的to比from小。
                    if (downloadInfo.cachedSize < downloadInfo.totalSize) {
                        saveBodyToFile(body, downloadInfo)
                    }
                    downloadInfo.updateStatusAndThrowable(STATUS_SUCCESSFUL)
                }
            } else if (response.code() == 416) {// 416表示请求的range超出范围。就表示已经下载完成了。不知道为什么，416错误有时候不能触发。难道是因为服务端不支持？
                downloadInfo.updateStatusAndThrowable(STATUS_SUCCESSFUL)
            } else {
                downloadInfo.updateStatusAndThrowable(
                    STATUS_FAILED,
                    RuntimeException("下载失败：code=${response.code()}")
                )
            }
        } catch (e: Exception) {
            if (isPaused) {
                downloadInfo.updateStatusAndThrowable(STATUS_PAUSED)
            } else {
                downloadInfo.updateStatusAndThrowable(STATUS_FAILED, e)
            }
        }
        postValue(downloadInfo)
    }

    /**
     * 把 ResponseBody 中的内容存储到 File中
     */
    private fun saveBodyToFile(body: ResponseBody, downloadInfo: DownloadInfo) {
        body.byteStream().use { inputStream ->
            RandomAccessFile(File(downloadInfo.downloadFileAbsolutePath), "rwd")
                .apply { seek(downloadInfo.cachedSize) }
                .use { outputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead = inputStream.read(buffer)
                    while (bytesRead >= 0) {
                        outputStream.write(buffer, 0, bytesRead)

                        downloadInfo.updateStatusAndThrowable(STATUS_RUNNING)
                        postValue(downloadInfo)

                        bytesRead = inputStream.read(buffer)
                    }
                }
        }
    }

    /**
     * 更新 [DownloadInfo] 的 [DownloadInfo.status] 和 [DownloadInfo.throwable]
     */
    private fun DownloadInfo.updateStatusAndThrowable(
        status: DownloadInfo.Status,
        throwable: Throwable? = null
    ) {
        this.status = status
        this.throwable = throwable
    }
}