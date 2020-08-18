package com.like.retrofit.sample

import com.google.gson.JsonObject
import com.like.retrofit.common.model.ApiResponse
import com.like.retrofit.livedata.CallLiveData
import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface Api {
    @Headers("cache:60")
    @GET("sys/randomImage/{key}")
    suspend fun getQueryMap(@Path("key") key: String): ResultModel<String?>

    @FormUrlEncoded
    @POST("/user/login")
    fun postField(@Field("username") username: String, @Field("password") password: String): Flowable<ResponseBody>

    @FormUrlEncoded
    @POST("/user/login")
    fun postFieldMap(@FieldMap data: @JvmSuppressWildcards Map<String, Any>): Flowable<ResponseBody>

    @Multipart
    @POST("/user/login")
    fun postPart(@Part("username") username: String, @Part("password") password: String): Flowable<ResponseBody>

    @Multipart
    @POST("/user/login")
    fun postPartMap(@PartMap data: @JvmSuppressWildcards Map<String, String>): Flowable<ResponseBody>

    @POST("/users/channels/internet-hospital/token/")
    fun postBody(@Body params: Map<String, String>): Flowable<ResponseBody>

    @POST("/users/channels/internet-hospital/token/")
    fun postBody(@Body params: JsonObject): Flowable<ResponseBody>

    @POST("/users/channels/internet-hospital/token/")
    fun postBodyMap(@Body paramsMap: @JvmSuppressWildcards Map<String, Any>): Flowable<ResponseBody>

    @POST("/users/channels/internet-hospital/token/")
    fun postBodyString(@Body params: String): Flowable<ResponseBody>

    @POST("/users/channels/internet-hospital/token/")
    fun postBodyResultModel(@Body params: ResultModel<String>): Flowable<ResponseBody>

    @GET("/banner/json")
    fun getCall(@QueryMap paramsMap: @JvmSuppressWildcards Map<String, Any>): Call<String>

    @GET("/banner/json")
    suspend fun getSuspend(@QueryMap paramsMap: @JvmSuppressWildcards Map<String, Any>): String

    @GET("/banner/json")
    fun getLiveData(@QueryMap paramsMap: @JvmSuppressWildcards Map<String, Any>): CallLiveData<ApiResponse<Any>>

}