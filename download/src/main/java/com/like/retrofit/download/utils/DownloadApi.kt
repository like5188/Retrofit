package com.like.retrofit.download.utils

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * retrofit2 支持的网络请求接口
 */

/** 注解：
 * 请求方法类：GET、POST、PUT、DELETE、PATCH、HEAD、OPTIONS、HTTP(可用于替代前面 7 个，及其他扩展方法；有 3 个属性：method、path、hasBody)
 * 标记类：FormUrlEncoded、Multipart、Streaming
 * 参数类：Headers、Header、Body、Field、FieldMap、Part、PartMap、Query、QueryMap、Path、URL。其中 Headers 作用于方法，其它注解作用于方法参数。
 *
 * FormUrlEncoded：请求体是 From 表单。Content-Type:application/x-www-form-urlencoded
 * Multipart：请求体是支持文件上传的 From 表单。Content-Type:multipart/form-data
 * Streaming：响应体的数据用流的形式返回。未使用该注解，默认会把数据全部载入内存，之后通过流获取数据也是读取内存中数据，所以返回数据较大时（比如下载大文件），需要使用该注解。
 *
 * Headers：设置固定的请求头，所有请求头不会相互覆盖，即使名字相同。比如：@Headers({ "Accept: application/vnd.github.v3.full+json","User-Agent: Retrofit-Sample-App"})
 * Header：动态更新请求头，匹配的参数必须提供给 @Header ，若参数值为 null ，这个头会被省略，否则，会使用参数值的 toString 方法的返回值。
 *
 * Body：post，非表单请求体，以 Post方式 传递 自定义数据类型 给服务器。
 * Content-Type 是根据传递的参数来确定的，比如：
 * String 类型的参数，对应 text/plain；这个需要添加 [retrofit2.converter.scalars.ScalarsConverterFactory] 转换器
 * Map、com.google.gson.JsonObject、实体类 类型的参数，对应 application/json；这个需要添加 [retrofit2.converter.gson.GsonConverterFactory] 转换器
 * 注意：
 * 1、这里如果用原生的 [org.json.JSONObject]，传递的参数会自动变成：{“nameValuePairs”:{}},这样会导致后端解析出错。所以参数传递 json 格式的话，就只能用 gson 中的 [com.google.gson.JsonObject]
 * 2、如果你在创建Retrofit对象时指定了retrofit2.Converter转换器，则会自动使用该转换器转换请求参数中的实体对象为RequestBody。如果没有指定转换器，则只能使用 RequestBody 传递请求参数。
 *
 * Field：post，表单字段。与 @FormUrlEncoded 注解配合使用
 * FieldMap：post，表单字段。与 @FormUrlEncoded 注解配合使用，接受 Map<String, String> 类型，非 String 类型会调用 toString() 方法
 *
 * Part、PartMap：post，表单字段。与 @ Multipart注解 配合使用。和Field 、 FieldMap的区别：功能相同，但携带的参数类型更加丰富。
 *      Part：支持的数据类型有 okhttp3.MultipartBody.Part、okhttp3.RequestBody、其它数据类型（需要 retrofit2.Converter 进行转换），所以上传文件只能用 @Part MultipartBody.Part 类型
 *      PartMap：支持一个Map作为参数，Map的value支持的数据类型有 okhttp3.RequestBody、其它数据类型（需要 retrofit2.Converter 进行转换）
 *
 * Query、QueryMap：get，用于查询参数（Query = Url 中 ‘?’ 后面的 key-value）。
 *
 * Path：URL地址的缺省值,例如:trades/{userId}
 * URL：直接传入一个请求的 URL变量 用于URL设置
 */

/** 返回值类型：
 * 1、Call<ResponseBody>：retrofit本身支持的。
// * 2、Flowable<ResponseBody>：添加 [retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory]后，支持把 Call<ResponseBody> 中的 Call 转换成 RxJava2 中的指定类型。
// * 3、LiveData<ApiResponse<ResponseBody>> 或者 CallLiveData<ApiResponse<ResponseBody>>：添加 [com.like.retrofit.common.factory.LiveDataCallAdapterFactory]后，支持把 Call<ResponseBody> 中的 Call 转换成 LiveDataCallAdapterFactory 中的指定类型。
 * 4、ResponseBody：当 fun 添加了 suspend 关键字后。其实前面3种相当于回调，这种相当于使用协程直接返回结果。
 *
 * 注意：如果添加了 [Converter.Factory]，那么可以把上面几种情况种的 ResponseBody 转换成 转换器种指定的类型。
 * 1、转换成 String 或者 8种基本数据类型：添加 [retrofit2.converter.scalars.ScalarsConverterFactory]
 * 2、转换成 实体类：添加 [retrofit2.converter.gson.GsonConverterFactory]
 */

/**
 * 注意：
 * 1、在kotlin代码中，必须用Map<String, @JvmSuppressWildcards Any>替代java中的Map<String, Object>。
 * 因为Retrofit在执行方法时，会检查Map的参数类型，Any类型会被转换成?(Object会被转换成java.lang.Object)，然后Retrofit又不允许通配符当作参数，
 * 所以会报错throw parameterError(p, “Parameter type must not include a type variable or wildcard: %s”, parameterType);
 * 详细描述看http://blog.csdn.net/hjkcghjmguyy/article/details/76408308
 *
 * 2、由于返回的实体类是通过Gson来解析的。所以此实体类如果有多余的字段，比如：
 * 下面的实体类：name是json中包含的数据；age是json中不包含的数据，是多余的final类型的字段。
 * 然而我们希望每个Info实例的默认值都为18。
 * 错误的写法，有构造函数：
 * data class Info(val name: String){
 *      val age = 18// Gson解析后不会给它赋值，所以它会变为默认值0。
 * }
 * 正确的写法，不要构造函数：
 * class Info{
 *      val name: String = ""
 *      val age = 18// Gson解析后会给它赋值，所以它还是18。
 * }
 * 因为Gson反序列化时：
 * 如果一个类没有默认构造函数，那么GSON是通过JDK内部API来创建对象实例的，并且通过反射给final字段赋值；
 * 如果有默认的构造函数，则通过反射调用默认构造函数创建实例。此时不会给final字段复制，那么age的值就会变成0了。
 *
 * 3、baseUrl 和 path 的关系
 * BaseUrl	                Path形式	    Path对应的值	                最后Url
 * http://host:port/a/b/	绝对路径	    /apath	                    http://host:port/apath
 * http://host:port/a/b/	相对路径	    apath	                    http://host:port/a/b/apath
 * http://host:port/a/b/	完整路径	    http://host:port/aa/apath	http://host:port/aa/apath
 */
interface DownloadApi {
    /**
     * 下载文件
     * 注意：添加了 suspend 后，返回结果会自动添加 Call<>，因为后面要用到 call 对象，所以这里不能加 suspend关键字。
     *
     * @param url   请求地址。可以是完整路径或者子路径
     * @param range 请求数据的范围。
     */
    @Streaming// 这个注解必须添加，否则文件全部写入内存，文件过大会造成内存溢出
    @GET
    suspend fun downloadFile(@Url url: String, @Header("RANGE") range: String): Response<ResponseBody>

    /**
     * 获取请求内容大小
     * 注意：添加了 suspend 后，返回结果会自动添加 Call<>。
     *
     * @param url   请求地址。可以是完整路径或者子路径
     */
    @Streaming// 这个注解必须添加，否则文件全部写入内存，文件过大会造成内存溢出
    @GET
    suspend fun getContentLength(@Url url: String): Long

}
