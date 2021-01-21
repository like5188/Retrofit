## 功能介绍

1、可以通过工具类：UploadRetrofit 上传文件。

## 使用方法：

1、初始化
```java
    val mUploadRetrofit = UploadRetrofit().init(RequestConfig(this@MyApplication))
```

2、上传文件。
```java
    var uploadJob: Job? = null
    uploadJob = lifecycleScope.launch(Dispatchers.Main) {
        mUploadRetrofit.uploadFiles(
            this,
            url,
            File("/storage/emulated/0/Pictures/WeiXin/test.jpg")
        ).collect {
            Log.i("MainActivity", it.toString())
        }
    }
    取消：
    uploadJob?.cancel()
```