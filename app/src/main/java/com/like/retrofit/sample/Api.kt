package com.like.retrofit.sample

import com.google.gson.JsonObject
import com.like.retrofit.common.model.ApiResponse
import com.like.retrofit.livedata.CallLiveData
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface Api {

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

    @POST("/users/channels/internet-hospital/token/")
    suspend fun postBody(@Body params: JsonObject): ResponseBody

    @POST("/users/channels/internet-hospital/token/")
    suspend fun postBodyMap(@Body paramsMap: @JvmSuppressWildcards Map<String, Any>): ResponseBody

    @POST("/users/channels/internet-hospital/token/")
    suspend fun postBodyString(@Body params: String): ResponseBody

    @POST("/users/channels/internet-hospital/token/")
    suspend fun postBodyResultModel(@Body params: ResultModel<String>): ResponseBody

    @Headers("cache:60")
    @GET("sys/randomImage/{key}")
    suspend fun getQueryMap(@Path("key") key: String): ResultModel<String?>

    @GET("/banner/json")
    fun getCall(@QueryMap paramsMap: @JvmSuppressWildcards Map<String, Any>): Call<String>

    @GET("/banner/json")
    fun getLiveData(@QueryMap paramsMap: @JvmSuppressWildcards Map<String, Any>): CallLiveData<ApiResponse<Any>>

}