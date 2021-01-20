package com.like.retrofit.upload.utils

import android.util.Log
import com.like.retrofit.upload.model.UploadInfo
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.io.File

/**
 * 通过progressLiveData返回进度的RequestBody
 *
 * Pair<Long, Long>：first为总长度，second为当前上传的进度
 */
internal class ProgressRequestBody(
    private val url: String,
    private val file: File,
    private val delegate: RequestBody
) : RequestBody() {
    var onProgress: ((UploadInfo) -> Unit)? = null
    private lateinit var bufferedSink: BufferedSink

    override fun contentLength(): Long = delegate.contentLength()

    override fun contentType(): MediaType? = delegate.contentType()

    override fun writeTo(sink: BufferedSink) {
        if (!::bufferedSink.isInitialized) {
            bufferedSink = object : ForwardingSink(sink) {
                // 当前已经上传的字节数
                var bytesWritten = 0L

                override fun write(source: Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    bytesWritten += byteCount

                    Log.v("MainActivity", "writeTo onProgress=$onProgress")
                    onProgress?.invoke(UploadInfo().apply {
                        this.url = this@ProgressRequestBody.url
                        this.totalSize = file.length()
                        this.absolutePath = file.absolutePath
                        this.status = UploadInfo.Status.STATUS_RUNNING
                        this.uploadSize = bytesWritten
                    })
                }
            }.buffer()
        }
        delegate.writeTo(bufferedSink)
        // 必须调用flush，否则最后一部分数据可能不会被写入
        bufferedSink.flush()
    }
}