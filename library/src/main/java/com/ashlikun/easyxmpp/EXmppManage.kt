package com.ashlikun.easyxmpp

import org.jivesoftware.smack.ReconnectionManager
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
        /**
         * 离线管理器
         */
        fun getOM(): OfflineMessageManager = get().offlineMessageManager

        /**
         * 重新连接管理器
         */
        fun getRM(): ReconnectionManager = get().reconnectionManager

        /**
         * 自己封装的聊天管理器
         */
        fun getChatM(): EXmppChatManage = EXmppChatManage.get()

        /**
         * 获取内部的连接管理器
         */
        fun getCM(): EXmppConnectionManage = get().connectionManage

        fun connect() {
            getCM().connect()
        }

        /**
         * 是否登录
         */
        fun isAuthenticated(): Boolean = getCM().isAuthenticated

        /**
         * 是否连接
         */
        fun isConnected(): Boolean = getCM().isConnected
    }


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
    lateinit var connectionManage: EXmppConnectionManage
        internal set

    fun getDomain(): String = connectionManage.domain


    internal fun init(configuration: XMPPTCPConnectionConfiguration, config: EasyXmppConfig) {
        this.config = config
        val connection = XMPPTCPConnection(configuration)
        //设置答复超时
        connection.replyTimeout = config.replyTimeout.toLong()
        connectionManage = EXmppConnectionManage(connection)
        reconnectionManager = ReconnectionManager.getInstanceFor(connection)
        offlineMessageManager = OfflineMessageManager(connection)
        EXmppChatManage.get()
        connectionManage.connect()
    }
}
