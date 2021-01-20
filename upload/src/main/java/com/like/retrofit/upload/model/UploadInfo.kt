package com.like.retrofit.upload.model

/**
 * 上传文件信息
 */
class UploadInfo {
    enum class Status {
        /**
         * when the upload is waiting to start.
         * 用户不会收到此状态。这只是为了表示初始状态
         */
        STATUS_PENDING,

        /**
         * when the upload is currently running.
         */
        STATUS_RUNNING,

        /**
         * when the upload has successfully completed.
         */
        STATUS_SUCCESS,

        /**
         * when the upload has failed (and will not be retried).
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
     * 上传文件的路径。
     */
    var absolutePath: String = ""

    /**
     * 上传地址
     */
    var url: String = ""

    /**
     * 已经上传的文件大小。
     */
    var uploadSize: Long = 0

    override fun toString(): String {
        return "DownloadInfo(uploadSize=$uploadSize, totalSize=$totalSize, status=$status, throwable=$throwable)"
    }

}