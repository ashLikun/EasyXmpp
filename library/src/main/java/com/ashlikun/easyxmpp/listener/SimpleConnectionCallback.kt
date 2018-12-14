package com.ashlikun.easyxmpp.listener

import com.ashlikun.easyxmpp.SmackInvocationException
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
open class SimpleConnectionCallback : ConnectionCallback {
    /**
     * 连接成功
     */
    override fun connected(connection: XMPPConnection) {

    }

    /**
     * 连接中断
     *@param isClose 是否是关闭连接
     * @param connection
     */
    override fun connectionError(isClose: Boolean, connection: SmackInvocationException) {

    }

    /**
     * 登录成功
     * @param resumed 是否是恢复的xmpp，true是
     */
    override fun authenticated(connection: XMPPConnection, resumed: Boolean) {

    }

}
