package com.like.retrofit.upload.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer

/**
 * 通过 progressLiveData 返回当前上传进度的 RequestBody
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class ProgressRequestBody(private val delegate: RequestBody) : RequestBody() {
    private val _controlCh = ConflatedBroadcastChannel<Long>()
    private lateinit var bufferedSink: BufferedSink

    internal fun getDataFlow() = _controlCh.asFlow()

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
                    _controlCh.trySend(bytesWritten)
                }
            }.buffer()
        }
        delegate.writeTo(bufferedSink)
        // 必须调用flush，否则最后一部分数据可能不会被写入
        bufferedSink.flush()
    }
}