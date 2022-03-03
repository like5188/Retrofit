package com.like.retrofit.util

import okhttp3.Interceptor
import java.io.IOException

/**
 * 网络没有连接异常
 * 注意：这里只能是继承 [IOException]，因为 okhttp 只会捕获发生在 [Interceptor] 中的 [IOException]。
 * 详情查看 [okhttp3.internal.connection.RealCall.AsyncCall.run]
 */
class NetworkException : IOException()