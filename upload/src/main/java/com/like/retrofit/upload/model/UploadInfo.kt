package com.like.retrofit.upload.model

class UploadInfo {
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
     * 当前状态
     */
    var status = Status.STATUS_PENDING

    /**
     * 当状态为[Status.STATUS_FAILED]时，对应的Throwable
     */
    var throwable: Throwable? = null

    /**
     * 上传的文件的路径。
     */
    var absolutePath: String = ""

    /**
     * 下载地址
     */
    var url: String = ""

    /**
     * 已经上传的文件大小。
     */
    var uploadedSize: Long = 0L

    override fun toString(): String {
        return "UploadInfo(absolutePath=$absolutePath, uploadedSize=$uploadedSize, totalSize=$totalSize, status=$status, throwable=$throwable)"
    }

}