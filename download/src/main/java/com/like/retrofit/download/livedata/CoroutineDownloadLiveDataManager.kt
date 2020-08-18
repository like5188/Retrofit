package com.like.retrofit.download.livedata

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import com.like.retrofit.download.model.DownloadInfo
import com.like.retrofit.download.model.DownloadInfo.Status.*
import com.like.retrofit.download.utils.merge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 所有子协程下载结果统一处理。包括：限制频率、过滤、汇总。
 * 如果需要对每个子协程的进度进行监控，请监听[mCoroutineDownloadLiveDataMap]中每个子协程的[CoroutineDownloadLiveData]。
 *
 * @param runningInterval   [com.like.retrofit.download.DownloadInfo.Status.STATUS_RUNNING]状态数据的发送频率
 */
@SuppressLint("MissingPermission")
class CoroutineDownloadLiveDataManager(
    private val mDownloadInfo: DownloadInfo,
    private val runningInterval: Long
) : DownloadLiveData() {

    // 持有子协程的CoroutineDownloadLiveData，可以让用户监控所有子协程的进度。
    private val mCoroutineDownloadLiveDataMap =
        mutableMapOf<CoroutineDownloadLiveData, Observer<DownloadInfo>>()

    private val isFirstPause = AtomicBoolean(true)
    private val isFirstSuccess = AtomicBoolean(true)
    private val isFirstFailed = AtomicBoolean(true)
    private var lastRunningTime = System.currentTimeMillis()

    override fun postValue(downloadInfo: DownloadInfo) {
        // 下载结果装饰。用于发送下载数据时进行额外处理。
        // 限制 STATUS_RUNNING 状态数据的发送频率
        if (downloadInfo.status != STATUS_RUNNING) {
            super.postValue(downloadInfo)
        } else if (System.currentTimeMillis() - lastRunningTime >= runningInterval) {
            super.postValue(downloadInfo)
            lastRunningTime = System.currentTimeMillis()
        }
    }

    override fun pause() {
        mCoroutineDownloadLiveDataMap.forEach {
            it.key.pause()
        }
    }

    override fun cancel() {
        mCoroutineDownloadLiveDataMap.forEach {
            it.key.cancel()
        }
        remove()
    }

    private fun remove() {
        mCoroutineDownloadLiveDataMap.forEach {
            it.key.removeObserver(it.value)
        }
    }

    fun hasLiveData() = mCoroutineDownloadLiveDataMap.isNotEmpty()

    fun getLiveDatas() = mCoroutineDownloadLiveDataMap.keys.toList()

    /**
     * @param liveData  子协程的CoroutineCallLiveData
     */
    suspend fun addLiveData(liveData: CoroutineDownloadLiveData) {
        val observer = createObserver()
        mCoroutineDownloadLiveDataMap[liveData] = observer
        withContext(Dispatchers.Main) {
            liveData.observeForever(observer)
        }
    }

    private fun createObserver() = Observer<DownloadInfo> { newDownloadInfo ->
        when (newDownloadInfo.status) {
            STATUS_PENDING -> {
            }
            STATUS_RUNNING -> {
                mDownloadInfo.updateStatusAndThrowable(STATUS_RUNNING)
                postValue(mDownloadInfo)
            }
            STATUS_PAUSED -> {
                // 只发射一次 STATUS_PAUSED 状态
                if (isFirstPause.compareAndSet(true, false)) {
                    mDownloadInfo.updateStatusAndThrowable(STATUS_PAUSED)
                    postValue(mDownloadInfo)
                    // remove()不能放到 pause()方法中，因为暂停是异步的。所以会导致收不到暂停状态的数据。
                    // 所以放到这里，在收到暂停状态的数据后再 remove()
                    remove()
                }
            }
            STATUS_SUCCESSFUL -> {
                if (mDownloadInfo.cachedSize != mDownloadInfo.totalSize) {// 保证所有协程都下载完成
                    mDownloadInfo.updateStatusAndThrowable(STATUS_RUNNING)
                    postValue(mDownloadInfo)
                } else {
                    // 如果在几个子文件都已经下载完成的情况，再次点击下载，
                    // 那么mDownloadInfo.cachedSize != mDownloadInfo.totalSize这个条件就不能限制只发送一次STATUS_SUCCESSFUL状态。
                    // 这时也会发送多个STATUS_SUCCESSFUL状态，只能用isFirstSuccess来保证只发送一次。
                    if (isFirstSuccess.compareAndSet(true, false)) {
                        // 合并文件，并删除子文件
                        (1..mDownloadInfo.threadCount)
                            .map { File("${mDownloadInfo.downloadFileAbsolutePath}.$it") }
                            .merge(File(mDownloadInfo.downloadFileAbsolutePath), true)

                        mDownloadInfo.updateStatusAndThrowable(STATUS_SUCCESSFUL)
                        postValue(mDownloadInfo)
                    }
                }
            }
            STATUS_FAILED -> {
                // 多协程导致多次收到错误状态数据，只取第一次的数据发送
                // （因为如果是某个协程运行出错，那么调用cancel()后，会导致其它协程返回socket closed(主动关闭)状态数据，
                // 这不是我们需要的，所以只取第一次的真实错误数据）
                if (isFirstFailed.compareAndSet(true, false)) {
                    mDownloadInfo.updateStatusAndThrowable(
                        newDownloadInfo.status,
                        newDownloadInfo.throwable
                    )
                    postValue(mDownloadInfo)
                    // 如果有一个 运行 出错，就退出所有的请求
                    cancel()
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