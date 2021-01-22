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
        val url = "xxx"
        val file = File("xxx")
        val progressBlock: (Flow<Long>) -> Unit = {
            launch {
                it.collect {
                    Log.d(TAG, "${Thread.currentThread().name} uploadedSize=$it  totalSize=${file.length()}")
                }
            }
        }
        try {
            val result = MyApplication.mUploadRetrofit.uploadFiles(url, mapOf(file to progressBlock))
            Log.i(TAG, result)
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "")
        }
    }
    取消：
    uploadJob?.cancel()
```