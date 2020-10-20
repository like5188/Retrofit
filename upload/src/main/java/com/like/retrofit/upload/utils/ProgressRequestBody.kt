package com.like.retrofit.upload.utils

import androidx.lifecycle.MutableLiveData
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer

/**
 * 通过progressLiveData返回进度的RequestBody
 *
 * Pair<Long, Long>：first为总长度，second为当前上传的进度
 */
internal class ProgressRequestBody(
    private val delegate: RequestBody,
    private val progressLiveData: MutableLiveData<Pair<Long, Long>>? = null
) : RequestBody() {
    private lateinit var bufferedSink: BufferedSink

    override fun contentLength(): Long = delegate.contentLength()

    override fun contentType(): MediaType? = delegate.contentType()

    override fun writeTo(sink: BufferedSink) {
        if (!::bufferedSink.isInitialized) {
            bufferedSink = object : ForwardingSink(sink) {
                // 总字节数
                val contentLength = contentLength()

                // 当前已经上传的字节数
                var bytesWritten = 0L

                override fun write(source: Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    bytesWritten += byteCount
                    progressLiveData?.postValue(Pair(contentLength, bytesWritten))
                }
            }.buffer()
        }
        delegate.writeTo(bufferedSink)
        // 必须调用flush，否则最后一部分数据可能不会被写入
        bufferedSink.flush()
    }
}