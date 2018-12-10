package com.ashlikun.easyxmpp

import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.offline.OfflineMessageManager
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager

/**
 * @author　　: 李坤
 * 创建时间: 2018/11/25 12:05
 * 邮箱　　：496546144@qq.com
 *
 *
 * 功能介绍：
 *
 *
 * 1:ChatStateManager: 聊天状态active（参加会话）, composing（正在输入）, gone（离开）, inactive（没有参加会话）, paused（暂停输入）。
 */

class XmppManage private constructor() {
    companion object {
        private val instance by lazy { XmppManage() }
        fun get(): XmppManage = instance
        /**
         * 离线管理器
         */
        fun getOM(): OfflineMessageManager = get().offlineMessageManager

        /**
         * 重新连接管理器
         */
        fun getRM(): EasyReconnectionManager = EasyReconnectionManager.getInstanceFor(getCM().connection)

        /**
         * 消息回执管理器
         */
        fun getDRM(): DeliveryReceiptManager = DeliveryReceiptManager.getInstanceFor(getCM().connection)

        /**
         * 自己封装的聊天管理器
         */
        fun getChatM(): EXmppChatManage = get().chatManage

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
     * 离线消息管理器
     */
    lateinit var offlineMessageManager: OfflineMessageManager
        internal set
    /**
     * 配置对象
     */
    lateinit var config: XmppConfig
        internal set
    /**
     * 内部的连接管理器
     */
    lateinit var connectionManage: EXmppConnectionManage
        internal set
    /**
     * 自己封装的聊天管理器
     */
    lateinit var chatManage: EXmppChatManage
        internal set

    fun getDomain(): String = connectionManage.domain


    /**
     * 全局初始化
     * 但是不会去连接服务器,也不会登录服务器
     */
    internal fun init(configuration: XMPPTCPConnectionConfiguration, config: XmppConfig) {
        this.config = config
        val connection = XMPPTCPConnection(configuration)
        //设置答复超时
        connection.replyTimeout = config.replyTimeout.toLong()

        //几个管理器封装
        connectionManage = EXmppConnectionManage(connection)
        offlineMessageManager = OfflineMessageManager(connection)
        chatManage = EXmppChatManage(connection)
        //自动重连
        EasyReconnectionManager.getInstanceFor(connection)
        //自动回执消息
        DeliveryReceiptManager.getInstanceFor(connection).autoAddDeliveryReceiptRequests()
    }
}
