package com.like.retrofit.sample

import kotlinx.coroutines.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() = runBlocking {
        GlobalScope.launch(MyContinuationInterceptor1() + MyContinuationInterceptor()) {
            log(1)
            val job = async {
                log(2)
                delay(1000)
                log(3)
                "Hello"
            }
            log(4)
            val result = job.await()
            log("5. $result")
        }.join()
        log(6)
    }

    inner class MyContinuationInterceptor : ContinuationInterceptor {
        override val key: CoroutineContext.Key<*> = ContinuationInterceptor

        override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
            return object : Continuation<T> {
                override val context: CoroutineContext = continuation.context

                override fun resumeWith(result: Result<T>) {
                    log("MyContinuation resumeWith $result")
                    continuation.resumeWith(result)
                }
            }
        }
    }

    inner class MyContinuationInterceptor1 : ContinuationInterceptor {
        override val key: CoroutineContext.Key<*> = ContinuationInterceptor

        override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
            return object : Continuation<T> {
                override val context: CoroutineContext = continuation.context

                override fun resumeWith(result: Result<T>) {
                    log("MyContinuation1 resumeWith $result")
                    continuation.resumeWith(result)
                }
            }
        }
    }

    suspend fun getUser() = suspendCoroutine<Any?> {
        log(11)
        it.resume(null)
    }

    private val dateFormat = SimpleDateFormat("HH:mm:ss:SSS")

    val now = {
        dateFormat.format(Date(System.currentTimeMillis()))
    }

    fun log(msg: Any?) = println("${now()} [${Thread.currentThread().name} ${Thread.currentThread().id}] $msg")
}
