package com.like.retrofit.sample

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    val flow1 = (1..2).asFlow().flatMapMerge {
        flow {
            delay(10)
            aaa(this)
            emit(111)
        }
    }

    private suspend fun aaa(flowCollector: FlowCollector<Int>) {
        delay(10)
        flowCollector.emit(222)
    }

    @Test
    fun addition_isCorrect() = runBlocking {
        flow {
            emit(1)
            emit(2)
            emitAll(flow1)
            emit(3)
        }.onEach {
            log(it)
        }.flowOn(Dispatchers.IO)
            .collect()
    }

    private val dateFormat = SimpleDateFormat("HH:mm:ss:SSS")

    val now = {
        dateFormat.format(Date(System.currentTimeMillis()))
    }

    fun log(msg: Any?) = println("${now()} [${Thread.currentThread().name} ${Thread.currentThread().id}] $msg")
}
