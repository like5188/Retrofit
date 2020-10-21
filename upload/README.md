## 功能介绍

1、可以通过工具类：UploadRetrofit 上传文件。

## 使用方法：

1、初始化
```java
    val mUploadRetrofit = UploadRetrofit().init(RequestConfig(this@MyApplication))
```

2、上传文件。
```java
    lifecycleScope.launch(Dispatchers.Main) {
        try {
            val file = File("/storage/emulated/0/DCIM/Camera/IMG_20201020_13423806.jpg")
            val result = MyApplication.mUploadRetrofit
                .uploadFiles(
                    "http://61.186.170.66:8800/xxc/sys/upload/temp/xxc/basket",
                    mapOf(file to {
                        launch {
                            it.collect {
                                Log.d(TAG, "${Thread.currentThread().name} totalSize=${it.first} uploadedSize=${it.second}")
                            }
                        }
                    })
                )
            Log.i(TAG, result)
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "")
        }
    }
```