package com.like.retrofit.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import com.like.retrofit.common.model.ApiEmptyResponse
import com.like.retrofit.common.model.ApiErrorResponse
import com.like.retrofit.common.model.ApiSuccessResponse
import com.like.retrofit.common.utils.await
import com.like.retrofit.common.utils.awaitApiResponse
import com.like.retrofit.download.model.DownloadInfo
import com.like.retrofit.download.utils.merge
import com.like.retrofit.download.utils.split
import com.like.retrofit.utils.bindToLifecycleOwner
import com.like.retrofit.utils.getCustomNetworkMessage
import kotlinx.coroutines.Dispatchers
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

    fun getCall(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val result = MyApplication.mCommonRetrofit.getService<Api>()
                    .getCall(mapOf("lastMondifiedTime" to 13399857800))
                    .await()
                Log.i(TAG, result)
            } catch (e: Exception) {
                Log.e(TAG, e.getCustomNetworkMessage())
            }
        }
    }

    fun getLiveData(view: View) {
        MyApplication.mCommonRetrofit.getService<Api>()
            .getLiveData(mapOf("lastMondifiedTime" to 13399857800))
            .bindToLifecycleOwner(this@MainActivity)
            ?.observe(this@MainActivity, Observer { apiResponse ->
                when (apiResponse) {
                    is ApiEmptyResponse -> {
                        Log.e(TAG, apiResponse.toString())
                    }
                    is ApiSuccessResponse -> {
                        Log.i(TAG, apiResponse.body.toString())
                    }
                    is ApiErrorResponse -> {
                        Log.e(TAG, apiResponse.throwable.getCustomNetworkMessage())
                    }
                }
            })
    }

    @SuppressLint("MissingPermission")
    fun uploadFiles(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            when (val apiResponse = MyApplication.mUploadRetrofit
                .uploadFiles("url", mapOf(File("../settings.gradle") to MutableLiveData()))
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
    }

    private var liveData: com.like.retrofit.download.livedata.DownloadLiveData? = null

    @SuppressLint("MissingPermission")
    fun download(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            liveData = MyApplication.mDownloadRetrofit.download(
                "https://qd.myapp.com/myapp/qqteam/pcqq/PCQQ2019.exe",
                File(cacheDir, "PCQQ2019.exe")
            )
            liveData?.observe(
                this@MainActivity,
                Observer<DownloadInfo> { downloadInfo ->
                    if (downloadInfo?.throwable != null) {
                        Log.e(TAG, downloadInfo.throwable.getCustomNetworkMessage())
                    } else {
                        Log.d(
                            TAG,
                            "[${Thread.currentThread().name} ${Thread.currentThread().id}] $downloadInfo"
                        )
                    }
                })
        }
    }

    fun pause(view: View) {
        liveData?.pause()
    }

    private var liveDataByMultiThread: com.like.retrofit.download.livedata.DownloadLiveData? = null

    @SuppressLint("MissingPermission")
    fun downloadByMultiThread(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            liveDataByMultiThread = MyApplication.mDownloadRetrofit.download(
                "https://qd.myapp.com/myapp/qqteam/pcqq/PCQQ2019.exe",
                File(cacheDir, "PCQQ2019.exe"),
                Runtime.getRuntime().availableProcessors()
            )
            liveDataByMultiThread?.observe(
                this@MainActivity,
                Observer<DownloadInfo> { downloadInfo ->
                    if (downloadInfo?.throwable != null) {
                        Log.e(TAG, downloadInfo.throwable.getCustomNetworkMessage())
                    } else {
                        Log.d(
                            TAG,
                            "[${Thread.currentThread().name} ${Thread.currentThread().id}] $downloadInfo"
                        )
                    }
                })
        }
    }

    fun pauseByMultiThread(view: View) {
        liveDataByMultiThread?.pause()
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
