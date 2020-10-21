## 功能介绍

1、可以通过工具类：CommonRetrofit 进行网络请求。

2、网络请求（都是在子线程中执行）支持的返回值类型：①、retrofit 原生支持的所有类型。 ②、LiveData<ApiResponse<T>> 。③、支持 kotlin 协程，直接在接口函数上添加 suspend 关键字。

## 使用方法：

1、根据自己的需求定义Api接口类
```java
    根据自己的需求定义Api接口类。
    interface Api {
        @Headers("cache:60")
        @GET("sys/randomImage/{key}")
        suspend fun getQueryMap(@Path("key") key: String): ResultModel<String?>
    }
```

2、初始化
```java
    val mCommonRetrofit = CommonRetrofit().init(
        RequestConfig(
            application = this@MyApplication,
            baseUrl = "http://xxx:xx/xxc/",
            interceptors = listOf(interceptor0, interceptor1)
        )
    )
```

3、进行普通网络请求。
```java
    lifecycleScope.launch(Dispatchers.Main) {
        try {
            val result = MyApplication.mCommonRetrofit.getService<Api>().getQueryMap("123")
            Log.i(TAG, result.toString())
        } catch (e: Exception) {
            Log.e(TAG, e.getCustomNetworkMessage())
        }
    }
```