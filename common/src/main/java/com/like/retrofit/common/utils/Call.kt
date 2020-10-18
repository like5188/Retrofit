package com.like.retrofit.common.utils

import com.like.retrofit.common.model.ApiResponse
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T : Any> Call<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                // runCatching 可以将一段代码的运行结果或者抛出的异常封装到一个 Result 类型当中
                continuation.resumeWith(runCatching {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body == null) {
                            val invocation = call.request().tag(Invocation::class.java)
                            val method = invocation?.method()
                            // Response from com.like.retrofit.sample.Api.getCall [{lastMondifiedTime=13399857800}] was null but response body type was declared as non-null
                            throw KotlinNullPointerException("Response from ${method?.declaringClass?.name}.${method?.name} ${invocation?.arguments()} was null but response body type was declared as non-null")
                        } else {
                            body
                        }
                    } else {
                        throw HttpException(response)
                    }
                })
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(t)
            }
        })
        registerOnCancellation(continuation)
    }
}

@JvmName("awaitNullable")
suspend fun <T : Any> Call<T?>.await(): T? {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<T?> {
            override fun onResponse(call: Call<T?>, response: Response<T?>) {
                continuation.resumeWith(runCatching {
                    if (response.isSuccessful) {
                        response.body()
                    } else {
                        throw HttpException(response)
                    }
                })
            }

            override fun onFailure(call: Call<T?>, t: Throwable) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(t)
            }
        })
        registerOnCancellation(continuation)
    }
}

suspend fun <T : Any> Call<T>.awaitResponse(): Response<T> {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(t)
            }
        })
        registerOnCancellation(continuation)
    }
}

/**
 * Suspend extension that allows suspend [Call] inside coroutine.
 *
 * @return sealed class [ApiResponse] object
 */
suspend fun <T : Any> Call<T>.awaitApiResponse(): ApiResponse<T> {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>?, response: Response<T>) {
                continuation.resume(ApiResponse.create(response))
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                // Don't bother with resuming the continuation if it is already cancelled.
                if (continuation.isCancelled) return
                continuation.resume(ApiResponse.create(t))
            }
        })

        registerOnCancellation(continuation)
    }
}

private fun Call<*>.registerOnCancellation(continuation: CancellableContinuation<*>) {
    continuation.invokeOnCancellation {
        try {
            cancel()
        } catch (ex: Throwable) {
            //Ignore cancel exception
        }
    }
}