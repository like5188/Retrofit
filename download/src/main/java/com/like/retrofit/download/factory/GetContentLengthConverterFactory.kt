package com.like.retrofit.download.factory

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.nio.charset.Charset

/**
 * Converter 有两个作用：
 * 1、处理接口的请求参数。比如转换用 @Body 标注的请求参数中的实体对象为 RequestBody，如果不指定转换器，则只能使用 RequestBody 传递请求参数。
 * 2、处理返回值 Call<ResponseBody> 中的 ResponseBody，把它转换成需要的类型。这里把返回的 ResponseBody 转换成 long 型的contentLength
 */
class GetContentLengthConverterFactory : Converter.Factory() {
    // 将API方法的返回类型从ResponseBody 转换为type，type是由CallAdapter 接口里面的responseType()函数返回的。
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, Long> {
        return ResponseBodyConverter()
    }

    /**
     * 采用了[GsonConverterFactory]一样的处理方式
     *
     * requestBodyConverter 方法 将API方法的输入参数类型从 type转换为ResponseBody ， 用于转换被注解@Body, @Part 和 @PartMap标记的类型
     * stringConverter 方法 将API方法的输入参数类型从 type 转换为String，用于转换被注解 @Header, @HeaderMap, @Path, @Query 和 @QueryMap 标记的类型
     */
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>, methodAnnotations: Array<Annotation>, retrofit: Retrofit
    ): Converter<*, RequestBody> {
        val gson = Gson()
        val adapter = gson.getAdapter(TypeToken.get(type))
        return RequestBodyConverter(
            gson,
            adapter
        )
    }

    class ResponseBodyConverter : Converter<ResponseBody, Long> {
        @Throws(IOException::class)
        override fun convert(responseBody: ResponseBody): Long {
            return responseBody.contentLength()// 获取内容的长度
        }
    }

    class RequestBodyConverter<T>(
        private val gson: Gson,
        private val adapter: TypeAdapter<T>
    ) : Converter<T, RequestBody> {

        private val MEDIA_TYPE = "application/json; charset=UTF-8".toMediaTypeOrNull()
        private val UTF_8 = Charset.forName("UTF-8")

        @Throws(IOException::class)
        override fun convert(value: T): RequestBody {
            val buffer = Buffer()
            val writer = OutputStreamWriter(buffer.outputStream(), UTF_8)
            val jsonWriter = gson.newJsonWriter(writer)
            adapter.write(jsonWriter, value)
            jsonWriter.close()
            return RequestBody.create(MEDIA_TYPE, buffer.readByteString())
        }
    }

}