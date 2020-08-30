## 功能介绍

1、可以通过工具类：CommonRetrofit 进行网络请求。

2、网络请求（都是在子线程中执行）支持的返回值类型：①、retrofit 原生支持的所有类型。 ②、LiveData<ApiResponse<T>> 。③、支持 kotlin 协程，直接在接口函数上添加 suspend 关键字。

## 使用方法：

1、根据自己的需求定义Api接口类
```java
    根据自己的需求定义Api接口类。
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
```

2、初始化
```java
    val mCommonRetrofit = CommonRetrofit().init(
        RequestConfig(
            application = this@MyApplication,
            baseUrl = "http://61.186.170.66:8800/xxc/",
            interceptors = listOf(interceptor0, interceptor1)
        )
    )
```

3、进行普通网络请求。
```java
    请求（在子线程运行）：(获取Api接口实例)
    mCommonRetrofit.getService<Api>().xxx()

    取消：调用扩展方法，会自动在LifecycleOwner的Lifecycle.Event指定的方法（默认onDestroy）调用时取消请求。
    @JvmOverloads
    fun <T> Call<T>?.bindToLifecycleOwner(
        lifecycleOwner: LifecycleOwner,
        vararg events: Lifecycle.Event = arrayOf(Lifecycle.Event.ON_DESTROY)
    ): Call<T>?

    @JvmOverloads
    fun <T> CallLiveData<T>?.bindToLifecycleOwner(
        lifecycleOwner: LifecycleOwner,
        vararg events: Lifecycle.Event = arrayOf(Lifecycle.Event.ON_DESTROY)
    ): CallLiveData<T>?

    @JvmOverloads
    fun <T> Job?.bindToLifecycleOwner(
        lifecycleOwner: LifecycleOwner,
        vararg events: Lifecycle.Event = arrayOf(Lifecycle.Event.ON_DESTROY)
    ): Job?

    例子：
    mCommonRetrofit.getService<Api>()
        .getQueryMap(0, mapOf("cid" to 60))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            {
                Log.i(TAG, "[${Thread.currentThread().name} ${Thread.currentThread().id}] $it")
            },
            {
                Log.e(
                    TAG,
                    "[${Thread.currentThread().name} ${Thread.currentThread().id}] ${it.getCustomNetworkMessage()}"
                )
            })
        .bindToLifecycleOwner(this@MainActivity)
```