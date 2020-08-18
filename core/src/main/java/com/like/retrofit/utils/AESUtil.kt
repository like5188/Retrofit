@file:Suppress("NOTHING_TO_INLINE")

package com.like.retrofit.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * AES加密
 *
 * @param key   加密用的Key。16位、32位。
 */
@Throws(Exception::class)
inline fun String.aesEncrypt(key: String): String {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(charset("utf-8")), "AES"))
    val bytes = toByteArray(charset("utf-8"))
    val aesBytes = cipher.doFinal(bytes)
    return Base64.encodeToString(aesBytes, Base64.DEFAULT)
}

/**
 * AES解密
 *
 * @param key   解密用的Key用的Key。16位、32位。
 */
@Throws(Exception::class)
inline fun String.aesDecrypt(key: String): String {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.toByteArray(charset("utf-8")), "AES"))
    val bytes = Base64.decode(this, Base64.DEFAULT)
    val aesBytes = cipher.doFinal(bytes)
    return aesBytes.toString(charset("utf-8"))
}