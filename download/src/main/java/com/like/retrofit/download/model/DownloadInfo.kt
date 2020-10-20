package com.like.retrofit.download.model

import java.io.File

class DownloadInfo {
    enum class Status {
        /**
         * when the download is waiting to start.
         * 用户不会收到此状态。这只是为了表示初始状态
         */
        STATUS_PENDING,

        /**
         * when the download is currently running.
         */
        STATUS_RUNNING,

        /**
         * when the download has successfully completed.
         */
        STATUS_SUCCESSFUL,

        /**
         * when the download has failed (and will not be retried).
         */
        STATUS_FAILED
    }

    /**
     * 文件的总大小
     */
    var totalSize: Long = 0

    /**
     * 当前的下载状态
     */
    var status = Status.STATUS_PENDING

    /**
     * 当状态为[Status.STATUS_FAILED]时，对应的Throwable
     */
    var throwable: Throwable? = null

    /**
     * 下载文件的路径。
     */
    var downloadFileAbsolutePath: String = ""

    /**
     * 下载地址
     */
    var url: String = ""

    /**
     * 分成几个子文件进行下载
     */
    var threadCount: Int = 1
        set(value) {
            if (value <= 0) {
                throw IllegalArgumentException("threadCount必须大于0")
            } else {
                field = value
            }
        }

    /**
     * 已经缓存的文件大小。
     * 单线程时：就是缓存文件的大小。
     * 多线程时：就是所有子线程缓存文件的大小之和。
     */
    val cachedSize: Long
        get() {
            var totalCachedSize = 0L
            try {
                if (downloadFileAbsolutePath.isNotEmpty()) {
                    val outFile = File(downloadFileAbsolutePath)
                    if (!outFile.isDirectory && outFile.exists()) {
                        totalCachedSize = outFile.length()
                    } else if (threadCount > 1) {
                        for (i in (1..threadCount)) {
                            val file = File("$downloadFileAbsolutePath.$i")
                            if (!file.isDirectory && file.exists()) {
                                totalCachedSize += file.length()
                            }
                        }

                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return totalCachedSize
        }

    override fun toString(): String {
        return "DownloadInfo(cachedSize=$cachedSize, totalSize=$totalSize, status=$status, throwable=$throwable, threadCount=$threadCount)"
    }

}