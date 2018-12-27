package com.ashlikun.easyxmpp.listener

import com.ashlikun.easyxmpp.SmackInvocationException
import com.ashlikun.easyxmpp.XmppManage
import com.ashlikun.easyxmpp.XmppUtils
import org.jivesoftware.smack.AbstractConnectionListener
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.filter.PresenceTypeFilter
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
     * 离线监听服务器叫我离线的事件,实现永不离线
     */
    private val offlineListener = StanzaListener {
        //离线了,重新上线
        XmppUtils.loge("当前用户离线了" + it.toString())
        //拉取离线消息
        offlineMessage()
        //设置状态在线
        XmppManage.getCM().userData.updateStateToAvailable()
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
            connection.removeStanzaSendingListener(offlineListener)
            connection.addStanzaSendingListener(offlineListener, PresenceTypeFilter.UNAVAILABLE)
        }
        XmppUtils.runMain {
            for (callback in callbackList) {
                callback.authenticated(connection, resumed)
            }
        }

    }

    private fun offlineMessage() {
        var size = XmppManage.getOM().messageCount
        XmppUtils.loge("离线消息一共$size")
        if (size > 0) {
            XmppManage.getOM().messages
        }
        XmppManage.getChatM().messageListener.cleanOfflineDeleteMessage()
        //通知服务器删除离线消息
        XmppManage.getOM().deleteMessages()
    }

    /**
     * 连接关闭
     */
    override fun connectionClosed() {
        super.connectionClosed()
        XmppUtils.loge("connectionClosed")
        XmppUtils.runMain {
            for (callback in callbackList) {
                callback.connectionError(true, SmackInvocationException("connection is closed"))
            }
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
        XmppUtils.runMain {
            for (callback in callbackList) {
                callback.connectionError(false, SmackInvocationException(e))
            }
        }
    }
}