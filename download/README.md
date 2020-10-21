## 功能介绍

1、可以通过工具类：DownloadRetrofit 下载文件（单协程、多协程）。

## 使用方法：

1、初始化
```java
    val mDownloadRetrofit = DownloadRetrofit().init(RequestConfig(this@MyApplication))
```

2、下载文件。
```java
    var downloadJob: Job? = null
    downloadJob = lifecycleScope.launch(Dispatchers.Main) {
        MyApplication.mDownloadRetrofit.download(url, File(cacheDir, "a.apk")).collect {
            if (it.throwable != null) {
                Log.e("Logger", it.throwable.getCustomNetworkMessage())
            } else {
                Log.d("Logger", it.toString())
            }
        }
    }
    取消：
    downloadJob?.cancel()
```