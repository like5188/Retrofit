package com.like.retrofit.common.factory

import androidx.lifecycle.LiveData
import com.like.retrofit.common.model.ApiResponse
import com.like.retrofit.livedata.CallLiveData
import retrofit2.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 支持接口返回值类型 Call<T> 中 Call 的转换：这里是把 Call<T> 转换成 LiveData<ApiResponse<T>>或者 CallLiveData<ApiResponse<T>>
 */
class LiveDataCallAdapterFactory : CallAdapter.Factory() {
    override fun get(
        returnType: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (!LiveData::class.java.isAssignableFrom(getRawType(returnType))) {
            return null
        }
        val responseType = getParameterUpperBound(0, returnType as ParameterizedType)
        val rawType = getRawType(responseType)
        if (rawType != ApiResponse::class.java) {
            throw IllegalArgumentException("LiveData return type must be parameterized as LiveData<ApiResponse> or LiveData<out ApiResponse>")
        }
        if (responseType !is ParameterizedType) {
            throw IllegalArgumentException("ApiResponse must be parameterized as ApiResponse<T> or ApiResponse<out T>")
        }
        val bodyType = getParameterUpperBound(0, responseType)
        return LiveDataCallAdapter<Any>(
            bodyType
        )
    }

    class LiveDataCallAdapter<T>(private val responseType: Type) :
        CallAdapter<T, CallLiveData<ApiResponse<T>>> {

        override fun responseType() = responseType

        override fun adapt(call: Call<T>): CallLiveData<ApiResponse<T>> =
            object : CallLiveData<ApiResponse<T>>(call) {
                private val started = AtomicBoolean(false)
                override fun onActive() {
                    super.onActive()
                    if (started.compareAndSet(false, true)) {
                        // Callback的onFailure()及onResponse()运行在非主线程。
                        call.enqueue(object : Callback<T> {
                            override fun onResponse(call: Call<T>, response: Response<T>) {
                                postValue(ApiResponse.create(response))
                            }

                            override fun onFailure(call: Call<T>, throwable: Throwable) {
                                postValue(ApiResponse.create(throwable))
                            }
                        })
                    }
                }
            }
    }

}