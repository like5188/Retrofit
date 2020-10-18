package com.like.retrofit.common.model

import retrofit2.HttpException
import retrofit2.Response

/**
 * 使用这个类是为了配合 LiveData 使用，因为 LiveData 无法发射错误信息
 *
 * Common class used by API responses.
 * @param <T> the type of the response object
</T> */
sealed class ApiResponse<T> {
    companion object {
        /**
         * 创建[ApiResponse]实例。包括[ApiErrorResponse]
         */
        fun <T> create(throwable: Throwable): ApiErrorResponse<T> {
            return ApiErrorResponse(throwable)
        }

        /**
         * 创建[ApiResponse]实例。包括[ApiEmptyResponse]、[ApiSuccessResponse]、[ApiErrorResponse]
         */
        fun <T> create(response: Response<T>): ApiResponse<T> {
            return if (response.isSuccessful) {
                val body = response.body()
                if (body == null || response.code() == 204) {
                    ApiEmptyResponse()
                } else {
                    ApiSuccessResponse(body)
                }
            } else {
                ApiErrorResponse(HttpException(response))
            }
        }
    }
}

/**
 * separate class for HTTP 204 responses so that we can make ApiSuccessResponse's body non-null.
 */
class ApiEmptyResponse<T> : ApiResponse<T>()

data class ApiSuccessResponse<T>(val body: T) : ApiResponse<T>()

data class ApiErrorResponse<T>(val throwable: Throwable) : ApiResponse<T>()