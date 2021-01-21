package com.like.retrofit.upload.utils

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface UploadApi {

    /**
     * 上传文件
     *
     * @param url       地址。可以是完整路径或者子路径
     * @param part      上传的文件
     * @param params    参数
     */
    @Multipart
    @POST
    suspend fun uploadFile(
        @Url url: String,
        @Part part: MultipartBody.Part,
        @PartMap params: @JvmSuppressWildcards Map<String, RequestBody>
    ): String

}
