package com.ashlikun.easyxmpp.listener

import com.ashlikun.easyxmpp.XmppUtils
import org.jivesoftware.smack.AbstractConnectionListener
import org.jivesoftware.smack.XMPPConnection
import java.util.*

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/30　10:16
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：
 */
class ExConnectionListener : AbstractConnectionListener() {
    var callbackList: MutableList<ConnectionCallback> = ArrayList()
    /**
     * 添加接受消息监听
     *
     * @param callback
     */
    fun addCallback(callback: ConnectionCallback) {
        if (!callbackList.contains(callback)) {
            callbackList.add(callback)
        }
    }

    fun removeCallback(callback: ConnectionCallback) {
        if (callbackList.contains(callback)) {
            callbackList.remove(callback)
        }
    }

    override fun connected(connection: XMPPConnection) {
        super.connected(connection)
        XmppUtils.loge("连接成功")
        for (callback in callbackList) {
            callback.connected(connection)
        }
    }

    /**
     * 登录成功
     *
     * @param connection
     * @param resumed
     */
    override fun authenticated(connection: XMPPConnection?, resumed: Boolean) {
        super.authenticated(connection, resumed)
        XmppUtils.loge("authenticated$resumed")
    }

    /**
     * 连接关闭
     */
    override fun connectionClosed() {
        super.connectionClosed()
        XmppUtils.loge("connectionClosed")
        for (callback in callbackList) {
            callback.connectionError(Exception("connection is closed"))
        }
    }

    /**
     * 连接错误
     *
     * @param e
     */
    override fun connectionClosedOnError(e: Exception) {
        super.connectionClosedOnError(e)
        XmppUtils.loge("connectionClosedOnError" + e.toString())
        for (callback in callbackList) {
            callback.connectionError(e)
        }
    }
}