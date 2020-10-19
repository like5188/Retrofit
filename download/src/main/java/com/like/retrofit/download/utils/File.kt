package com.like.retrofit.download.utils

import android.Manifest
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.RandomAccessFile

/**
 * 如果file不存在，则创建。
 */
@SuppressLint("MissingPermission")
@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
fun File.create(): Boolean = try {
    if (!this.isDirectory) {// 是文件
        if (!this.exists()) {
            val parentFile = this.parentFile
            if (parentFile != null && !parentFile.exists()) {// 如果父目录不存在
                parentFile.mkdirs()// 创建目录
            }
            this.createNewFile()// 创建文件
        }
    } else if (!this.exists()) {// 是目录，并且不存在
        this.mkdirs()// 创建目录
    }
    true
} catch (e: Exception) {
    e.printStackTrace()
    false
}

/**
 * 分割文件
 *
 * @param count 分割数量
 */
@SuppressLint("MissingPermission")
@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
fun File?.split(count: Int): List<File>? {
    if (this == null || this.isDirectory || !this.exists()) {
        return null
    }
    if (count <= 0) {
        throw IllegalArgumentException("分割数量count必须大于0")
    }
    if (this.length() < count) {
        throw IllegalArgumentException("文件 ${this.name} 太小，不能分割为 $count 个小文件")
    }
    val fileLength = this.length()
    // 每个子文件的大小。最后一个子文件的大小<=blockSize
    val blockSize = if (fileLength % count == 0L) {
        fileLength / count
    } else {
        fileLength / count + 1
    }.toInt()
    val bufferSize = if (DEFAULT_BUFFER_SIZE > blockSize) blockSize else DEFAULT_BUFFER_SIZE
    val buffer = ByteArray(bufferSize)
    val files = mutableListOf<File>()
    RandomAccessFile(this, "rw").use { input ->
        for (i in (1..count)) {
            val outFile = File("${this.absolutePath}.$i")
            outFile.createNewFile()
            RandomAccessFile(outFile, "rw").use { output ->
                // 如果blockSize>DEFAULT_BUFFER_SIZE时，会循环读取。
                var bytesRead = input.read(buffer)
                while (bytesRead >= 0) {
                    output.write(buffer, 0, bytesRead)
                    val remaining = blockSize.toLong() - output.length()
                    if (remaining == 0L) {
                        break
                    } else {
                        // 循环读取时，用精确的缓存大小来控制每个子文件的大小为blockSize。
                        bytesRead =
                            input.read(
                                buffer,
                                0,
                                if (remaining > bufferSize) bufferSize else remaining.toInt()
                            )
                    }
                }
            }
            files.add(outFile)
        }
    }
    return files
}

/**
 * 合并按顺序排列好的子文件列表到一个指定文件
 *
 * @param outFile       合并后的文件
 * @param deleteFiles   合并成功后，是否删除子文件
 */
@SuppressLint("MissingPermission")
@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
fun List<File>?.merge(outFile: File?, deleteFiles: Boolean = false) {
    if (this == null || this.isEmpty()) {
        return
    }
    if (outFile == null || outFile.isDirectory || !outFile.create()) {
        return
    }
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    RandomAccessFile(outFile, "rw").use { output ->
        this.forEach {
            RandomAccessFile(it, "rw").use { input ->
                var bytesRead = input.read(buffer)
                while (bytesRead >= 0) {
                    output.write(buffer, 0, bytesRead)
                    bytesRead = input.read(buffer)
                }
            }
            if (deleteFiles) it.delete()
        }
    }
}