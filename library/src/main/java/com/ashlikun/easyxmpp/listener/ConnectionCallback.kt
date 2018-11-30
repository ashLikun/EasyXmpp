package com.ashlikun.easyxmpp.listener

import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.XMPPConnection

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　15:18
 * 邮箱　　：496546144@qq.com
 *
 *
 * 功能介绍：提供给外部监听,对应连接的状态
 * 基于[ConnectionListener]
 */
interface ConnectionCallback {
    /**
     * 连接成功
     */
    fun connected(connection: XMPPConnection)

    /**
     * 连接中断
     *
     * @param connection
     */
    fun connectionError(connection: Exception)

}
