package com.ashlikun.easyxmpp

import org.jivesoftware.smack.ReconnectionManager
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.offline.OfflineMessageManager

/**
 * @author　　: 李坤
 * 创建时间: 2018/11/25 12:05
 * 邮箱　　：496546144@qq.com
 *
 *
 * 功能介绍：
 */

class EXmppManage private constructor() {
    companion object {
        private val instance by lazy { EXmppManage() }
        fun get(): EXmppManage = instance
        fun connect() {
            get().cm.connect()
        }

        /**
         * 是否登录
         */
        fun isAuthenticated(): Boolean = get().cm.isAuthenticated

        /**
         * 是否连接
         */
        fun isConnected(): Boolean = get().cm.isConnected
    }

    /**
     * 获取聊天管理器
     */
    lateinit var chatManager: ChatManager
        internal set

    /**
     * 重新连接管理器
     */
    lateinit var reconnectionManager: ReconnectionManager
        internal set
    /**
     * 离线消息管理器
     */
    lateinit var offlineMessageManager: OfflineMessageManager
        internal set
    /**
     * 配置对象
     */
    lateinit var config: EasyXmppConfig
        internal set
    /**
     * 获取内部的连接管理器
     */
    lateinit var cm: EXmppConnectionManage
        internal set

    val domain: String
        get() = cm.domain


    internal fun init(configuration: XMPPTCPConnectionConfiguration, config: EasyXmppConfig) {
        this.config = config
        val connection = XMPPTCPConnection(configuration)
        //设置答复超时
        connection.replyTimeout = config.replyTimeout.toLong()
        cm = EXmppConnectionManage(connection)
        connection.addConnectionListener(cm)
        chatManager = ChatManager.getInstanceFor(connection)
        reconnectionManager = ReconnectionManager.getInstanceFor(connection)
        offlineMessageManager = OfflineMessageManager(connection)
        cm.connect()
    }
}
