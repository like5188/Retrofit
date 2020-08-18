## 功能介绍

1、可以通过工具类：UploadRetrofit 上传文件。

## 使用方法：

1、初始化
```java
    val mUploadRetrofit = UploadRetrofit().init(RequestConfig(this@MyApplication))
```

2、上传文件。
```java
    GlobalScope.launch(Main) {
        when (val apiResponse = mUploadRetrofit.uploadFiles("url", mapOf(File("../settings.gradle") to MutableLiveData()))
            .awaitApiResponse()) {
            is ApiEmptyResponse -> {
                Log.d(TAG, "2 ${Thread.currentThread().name}")
                Log.e(TAG, apiResponse.toString())
            }
            is ApiSuccessResponse -> {
                Log.d(TAG, "3 ${Thread.currentThread().name}")
                Log.i(TAG, apiResponse.body.toString())
            }
            is ApiErrorResponse -> {
                Log.d(TAG, "4 ${Thread.currentThread().name}")
                Log.e(TAG, apiResponse.throwable.getCustomNetworkMessage())
            }
            else -> {
            }
        }
    }
```