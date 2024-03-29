package com.like.retrofit.sample

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.http.*

interface Api {
    // 缓存单位为秒
    @Headers("cache:10")
    @GET("maven_pom/package/json")
    suspend fun get(): String

    @FormUrlEncoded
    @POST("/user/login")
    suspend fun postField(@Field("username") username: String, @Field("password") password: String): ResponseBody

    @FormUrlEncoded
    @POST("/user/login")
    suspend fun postFieldMap(@FieldMap data: @JvmSuppressWildcards Map<String, Any>): ResponseBody

    @Multipart
    @POST("/user/login")
    suspend fun postPart(@Part("username") username: String, @Part("password") password: String): ResponseBody

    @Multipart
    @POST("/user/login")
    suspend fun postPartMap(@PartMap data: @JvmSuppressWildcards Map<String, String>): ResponseBody

    @POST("https://kcwcdev.i.cacf.cn/api/user/1.0.0/login")
    suspend fun postBody(@Body params: JsonObject): ResponseBody

    @POST("/users/channels/internet-hospital/token/")
    suspend fun postBodyMap(@Body paramsMap: @JvmSuppressWildcards Map<String, Any>): ResponseBody

    @POST("/users/channels/internet-hospital/token/")
    suspend fun postBodyString(@Body params: String): ResponseBody

    @POST("/users/channels/internet-hospital/token/")
    suspend fun postBodyResultModel(@Body params: ResultModel<String>): ResponseBody

    @GET("sys/randomImage/{key}")
    suspend fun getQueryMap(@Path("key") key: String): ResultModel<String?>

}