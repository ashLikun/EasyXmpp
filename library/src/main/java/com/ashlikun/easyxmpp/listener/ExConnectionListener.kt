package com.ashlikun.easyxmpp.listener

import com.ashlikun.easyxmpp.XmppManage
import com.ashlikun.easyxmpp.XmppUtils
import org.jivesoftware.smack.AbstractConnectionListener
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.packet.Presence
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
     * 添加连接监听
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
     * 登录成功,处理离线消息
     *
     * @param connection
     * @param resumed
     */
    override fun authenticated(connection: XMPPConnection, resumed: Boolean) {
        super.authenticated(connection, resumed)
        XmppUtils.loge("authenticated$resumed")
        //如果没有设置登录状态
        if (!XmppManage.get().config.sendPresence && !resumed) {
            XmppUtils.runNew {
                XmppManage.getRM().isReconnectUnavailable = false
                //设置离线状态
                XmppManage.getCM().userData?.updateState(Presence.Type.unavailable)
                //设置状态在线
                XmppManage.getCM().userData?.updateState(Presence.Type.available)
            }
        }
        for (callback in callbackList) {
            callback.authenticated(connection, resumed)
        }
    }

    /**
     * 连接关闭
     */
    override fun connectionClosed() {
        super.connectionClosed()
        XmppUtils.loge("connectionClosed")
        for (callback in callbackList) {
            callback.connectionError(true, Exception("connection is closed"))
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
            callback.connectionError(false, e)
        }
    }
}