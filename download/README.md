## 功能介绍

1、可以通过工具类：DownloadRetrofit 下载文件（单协程、多协程）。

## 使用方法：

1、初始化
```java
    val mDownloadRetrofit = DownloadRetrofit().init(RequestConfig(this@MyApplication))
```

2、下载文件。
```java
    private var liveData: DownloadLiveData? = null
    GlobalScope.launch(Main) {
        val file = File(cacheDir, "com.snda.wifilocating_181219.apk")
        liveData = mDownloadRetrofit.download(
            "http://shouji.360tpcdn.com/181222/f31d56919d5dfbdb479a8e5746a75146/com.snda.wifilocating_181219.apk",
            file,
            Runtime.getRuntime().availableProcessors()
        )
        liveData?.observe(this@MainActivity, Observer<DownloadInfo> { downloadInfo ->
            if (downloadInfo?.throwable != null) {
                Log.e(TAG, downloadInfo.throwable.getCustomNetworkMessage())
            } else {
                Log.d(
                    "MainActivity",
                    "[${Thread.currentThread().name} ${Thread.currentThread().id}] $downloadInfo"
                )
            }
        })
    }
    暂停：
    liveData?.pause()
    取消：
    liveData?.cancel()
```