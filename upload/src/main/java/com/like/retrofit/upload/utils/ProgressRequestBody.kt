package com.like.retrofit.upload.utils

import androidx.lifecycle.MutableLiveData
import com.like.retrofit.upload.model.UploadInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.io.File

/**
 * 通过 liveData 返回进度的 RequestBody
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ProgressRequestBody(
    private val liveData: MutableLiveData<UploadInfo>,
    private val url: String,
    private val file: File,
    private val delegate: RequestBody
) : RequestBody() {
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
                    liveData.postValue(UploadInfo().apply {
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