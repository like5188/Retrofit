package com.like.retrofit.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object GsonUtils {
    private val mGson = GsonBuilder().create()
    private val mJsonParser = JsonParser()

    fun combineToJsonObject(vararg objects: Any?): JsonObject? {
        if (objects.isNullOrEmpty()) {
            return null
        }
        return try {
            val builder = StringBuilder()
            for (o in objects) {
                if (o != null) {
                    val s: String = mGson.toJson(o)
                    if (builder.isNotEmpty()) {
                        builder.append(s)
                        builder.deleteCharAt(builder.length - s.length)
                    } else {
                        builder.append(s)
                    }
                    builder.deleteCharAt(builder.length - 1)
                    builder.append(",")
                }
            }
            if (builder.isEmpty()) {
                null
            } else {
                builder.deleteCharAt(builder.length - 1)
                builder.append("}")
                mJsonParser.parse(builder.toString()).asJsonObject
            }
        } catch (e: Exception) {
            null
        }
    }

    fun toJsonString(any: Any?): String? {
        if (any == null) return null
        return try {
            mGson.toJson(any)
        } catch (e: Exception) {
            null
        }
    }

    fun toJsonObject(any: Any?): JsonObject? {
        if (any == null) return null
        return try {
            mGson.toJsonTree(any).asJsonObject
        } catch (e: Exception) {
            null
        }
    }

}