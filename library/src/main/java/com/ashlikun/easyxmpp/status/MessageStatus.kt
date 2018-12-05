package com.ashlikun.easyxmpp.status

import android.support.annotation.IntDef


/**
 * @author　　: 李坤
 * 创建时间: 2018/11/29 9:34
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：消息状态
 */
object MessageStatus {
    /**
     * 发送中
     */
    const val SENDING = 0
    /**
     * 文件发送成功（http） 但是消息还是在发送中
     * 如图片或者语音或者文件的本地路径
     */
    const val FILE_SUCCESS = 1
    /**
     * 发送成功
     */
    const val SUCCESS = 2
    /**
     * 发送失败
     */
    const val ERROR = 3


    @IntDef(value = [SENDING, SUCCESS, ERROR])
    @Retention(AnnotationRetention.SOURCE)
    annotation class Code

    /**
     * 正在发送中
     */
    fun isSendIng(@Code code: Int): Boolean {
        return code == MessageStatus.SENDING || code == MessageStatus.FILE_SUCCESS
    }

    /**
     * 文件发送成功
     */
    fun isFileSuccess(@Code code: Int): Boolean {
        return code == MessageStatus.FILE_SUCCESS
    }

    /**
     * 发送完成
     */
    fun isSuccess(@Code code: Int): Boolean {
        return code == MessageStatus.SUCCESS
    }

    /**
     * 发送错误
     */
    fun isError(@Code code: Int): Boolean {
        return code == MessageStatus.ERROR
    }
}
