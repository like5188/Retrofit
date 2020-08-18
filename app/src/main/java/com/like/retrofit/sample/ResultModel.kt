package com.like.retrofit.sample

data class ResultModel<T>(
    val code: Int = -1,
    val message: String? = null,
    val result: T? = null,
    val success: Boolean = false
)