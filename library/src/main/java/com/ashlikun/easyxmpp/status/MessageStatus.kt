package com.ashlikun.easyxmpp.status

import android.icu.text.DateTimePatternGenerator.PatternInfo.OK
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
     * 发送成功
     */
    const val SUCCESS = 1
    /**
     * 发送失败
     */
    const val ERROR = 2


    @IntDef(value = [SENDING, SUCCESS, ERROR])
    @Retention(AnnotationRetention.SOURCE)
    annotation class Code

    fun isIng(@Code code: Int): Boolean {
        return code == MessageStatus.SENDING
    }

    fun isSuccess(@Code code: Int): Boolean {
        return code == MessageStatus.SUCCESS
    }

    fun isError(@Code code: Int): Boolean {
        return code == MessageStatus.ERROR
    }
}
