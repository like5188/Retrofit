package com.like.retrofit.utils

import okhttp3.RequestBody
import okio.Buffer

fun RequestBody?.string(): String {
    try {
        if (this != null) {
            val buffer = Buffer()
            this.writeTo(buffer)
            return buffer.readUtf8()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}