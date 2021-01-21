package com.like.retrofit.sample

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import com.like.retrofit.download.utils.merge
import com.like.retrofit.download.utils.split
import com.like.retrofit.util.getCustomNetworkMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun getQueryMap(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val result = MyApplication.mCommonRetrofit.getService<Api>().getQueryMap("123")
                Log.i(TAG, result.toString())
            } catch (e: Exception) {
                Log.e(TAG, e.getCustomNetworkMessage())
            }
        }
    }

    fun postField(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val responseBody = MyApplication.mCommonRetrofit.getService<Api>()
                    .postField("like5188", "like5488")
                Log.i(TAG, responseBody.string())
            } catch (e: Exception) {
                Log.e(TAG, e.getCustomNetworkMessage())
            }
        }
    }

    fun postFieldMap(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val responseBody = MyApplication.mCommonRetrofit.getService<Api>()
                    .postFieldMap(mapOf("username" to "like5188", "password" to "like5488"))
                Log.i(TAG, responseBody.string())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun postPart(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val responseBody = MyApplication.mCommonRetrofit.getService<Api>()
                    .postPart("like5188", "like5488")
                Log.i(TAG, responseBody.string())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun postPartMap(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val responseBody = MyApplication.mCommonRetrofit.getService<Api>()
                    .postPartMap(mapOf("username" to "like5188", "password" to "like5488"))
                Log.i(TAG, responseBody.string())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun postBody(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // 注意：这里不能使用 org.json.JSONObject 类，
                // 会被 GsonConverterFactory 转换成 {"nameValuePairs":{"username":"13508129810","password":"123456"}} 导致后台不能识别。
                val params = JsonObject()
                params.addProperty("username", "13508129810")
                params.addProperty("password", "123456")
                val responseBody = MyApplication.mCommonRetrofit.getService<Api>()
                    .postBody(params)
//            .postBody(mapOf("username" to "13508129810", "password" to "123456"))
                Log.i(TAG, responseBody.string())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun postBodyMap(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val responseBody = MyApplication.mCommonRetrofit.getService<Api>()
                    .postBodyMap(mapOf("username" to "13508129810", "password" to "123456"))
                Log.i(TAG, responseBody.string())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun postBodyString(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val responseBody = MyApplication.mCommonRetrofit.getService<Api>()
                    .postBodyString("123")
                Log.i(TAG, responseBody.string())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun postBodyResultModel(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val responseBody = MyApplication.mCommonRetrofit.getService<Api>()
                    .postBodyResultModel(ResultModel(1, "", "2", true))
                Log.i(TAG, responseBody.string())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var uploadJob: Job? = null

    @SuppressLint("MissingPermission")
    fun uploadFiles(view: View) {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                val url = "http://61.186.170.66:8800/xxc/sys/upload/temp/xxc/basket"
                uploadJob = lifecycleScope.launch(Dispatchers.Main) {
                    MyApplication.mUploadRetrofit.uploadFiles(
                        this,
                        url,
                        File("/storage/emulated/0/Pictures/WeiXin/test.jpg")
                    ).collect {
                        Log.i("MainActivity", it.toString())
                    }
                }
            }
        }.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun cancelUploadFiles(view: View) {
        uploadJob?.cancel()
    }

    var downloadJob: Job? = null

    @SuppressLint("MissingPermission")
    fun download(view: View) {
//        val url = "https://imtt.dd.qq.com/16891/apk/91059321573A1E1BFF5BC3235A9ABC35.apk"//大文件
        val url = "https://imtt.dd.qq.com/16891/apk/8409D55AE4A1DB11320E466C427FD2E2.apk"//小文件
        downloadJob = lifecycleScope.launch(Dispatchers.Main) {
            MyApplication.mDownloadRetrofit.downloadFile(
                url,
                File(cacheDir, "a.apk"),
                deleteCache = true
            ).collect {
                Log.i("MainActivity", it.toString())
            }
        }
    }

    fun pause(view: View) {
        downloadJob?.cancel()
    }

    @SuppressLint("MissingPermission")
    fun downloadByMultiThread(view: View) {
        val url = "https://imtt.dd.qq.com/16891/apk/91059321573A1E1BFF5BC3235A9ABC35.apk"//大文件
//        val url = "https://imtt.dd.qq.com/16891/apk/8409D55AE4A1DB11320E466C427FD2E2.apk"//小文件
        downloadJob = lifecycleScope.launch(Dispatchers.Main) {
            MyApplication.mDownloadRetrofit.downloadFile(
                url,
                File(cacheDir, "a.apk"),
                Runtime.getRuntime().availableProcessors(),
                deleteCache = false
            ).collect {
                Log.i("MainActivity", it.toString())
            }
        }
    }

    fun pauseByMultiThread(view: View) {
        downloadJob?.cancel()
    }

    @SuppressLint("MissingPermission")
    fun split(view: View) {
        val file = File(cacheDir, "TestMulti.zip")
        var totalSize = 0L
        file.split(4)?.forEach {
            Log.d(TAG, "fileName=${it.name} fileLength=${it.length()} filePath=${it.absolutePath}")
            totalSize += it.length()
        }
        Log.w(TAG, "originSize=${file.length()} totalSize=$totalSize")
    }

    @SuppressLint("MissingPermission")
    fun merge(view: View) {
        val outFile = File(cacheDir, "TestMulti.zip")
        listOf(
            File(cacheDir, "TestMulti.zip.1"),
            File(cacheDir, "TestMulti.zip.2"),
            File(cacheDir, "TestMulti.zip.3"),
            File(cacheDir, "TestMulti.zip.4")
        ).merge(outFile)
        Log.d(
            TAG,
            "outFileName=${outFile.name} outFileLength=${outFile.length()} outFilePath=${outFile.absolutePath}"
        )
    }

}
