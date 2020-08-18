package com.like.retrofit.download.livedata

import com.like.retrofit.livedata.CallLiveData
import com.like.retrofit.download.model.DownloadInfo
import okhttp3.ResponseBody
import retrofit2.Call

/**
 * 支持暂停操作。
 */
open class DownloadLiveData(call: Call<ResponseBody>? = null) : CallLiveData<DownloadInfo>(call) {
    @Volatile
    var isPaused = false

    open fun pause() {
        isPaused = true
        cancel()
    }

}