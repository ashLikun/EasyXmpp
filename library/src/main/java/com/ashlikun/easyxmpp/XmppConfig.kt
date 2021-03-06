package com.ashlikun.easyxmpp

import android.app.Application
import android.text.TextUtils
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.ping.PingManager
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager
import org.jxmpp.stringprep.XmppStringprepException

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　11:14
 * 邮箱　　：496546144@qq.com
 *
 *
 * 功能介绍：对xmpp的初始化配置
 */
class XmppConfig {

    lateinit var application: Application
        internal set
    /**
     * 服务器名称
     */
    lateinit var host: String
        internal set
    /**
     * domain,默认就是服务器名称
     * 通常表示一个特定的会话（与某个设备），连接（与某个地址），或者一个附属于某个节点ID实体相关实体的对象（比如多用户聊天室中的一个参加者）
     */
    var domain: String? = null
        internal set
    /**
     * 端口 默认5222
     */
    var port = 5222
        internal set
    /**
     * 设置端名称
     */
    var resource = "android"
        internal set
    /**
     * 设置连接最大超时时间
     */
    var connectTimeout = 20000
        internal set
    /**
     * 安全模式
     */
    var securityMode: ConnectionConfiguration.SecurityMode = ConnectionConfiguration.SecurityMode.disabled
        internal set
    /**
     * 消息回执类型
     */
    var receipt: DeliveryReceiptManager.AutoReceiptMode = DeliveryReceiptManager.AutoReceiptMode.disabled
        internal set
    /**
     * 是否开启压缩
     */
    var compressionEnabled = true
        internal set
    /**
     * 设置登录状态,不想在线传false
     */
    var sendPresence = true
        internal set
    /**
     * 是否调试
     */
    var isDebug = true
        internal set
    /**
     * 需要经过同意才可以添加好友 manual
     * 添加直接通过accept_all
     */
    var subscriptionMode: Roster.SubscriptionMode = Roster.SubscriptionMode.accept_all
        internal set
    /**
     * 设置答复超时
     */
    var replyTimeout = 5000
        internal set
    /**
     * 定义如何处理传出节的from属性。默认由服务端指定
     */
    var fromMode: XMPPConnection.FromMode? = null
        internal set
    /**
     * 重新连接时间间隔
     */
    var reconnectionTime = 5000
        internal set
    /**
     * 心跳ping间隔
     */
    var pingInterval = 60000
        internal set
    /**
     * 是否重新连接
     *
     * @return
     */
    var isReconnection = true
        internal set

    class Builder private constructor(application: Application) {
        internal var config: XmppConfig

        init {
            config = XmppConfig()
            config.application = application
        }

        /**
         * 服务器名称
         */
        fun host(host: String): Builder {
            config.host = host
            return this
        }

        /**
         * domain,默认就是服务器名称
         */
        fun domain(domain: String): Builder {
            config.domain = domain
            return this
        }

        /**
         * 端口 默认5222
         */
        fun port(port: Int): Builder {
            config.port = port
            return this
        }

        /**
         * 设置端名称
         */
        fun resource(resource: String): Builder {
            config.resource = resource
            return this
        }

        /**
         * 设置连接最大超时时间
         */
        fun connectTimeout(connectTimeout: Int): Builder {
            config.connectTimeout = connectTimeout
            return this
        }

        /**
         * 安全模式
         */
        fun securityMode(securityMode: ConnectionConfiguration.SecurityMode): Builder {
            config.securityMode = securityMode
            return this
        }

        /**
         * 是否开启压缩
         */
        fun compressionEnabled(compressionEnabled: Boolean): Builder {
            config.compressionEnabled = compressionEnabled
            return this
        }

        /**
         * 设置登录状态,不想登录传false
         */
        fun sendPresence(sendPresence: Boolean): Builder {
            config.sendPresence = sendPresence
            return this
        }

        /**
         * 是否调试
         */
        fun isDebug(isDebug: Boolean): Builder {
            config.isDebug = isDebug
            return this
        }

        /**
         * 需要经过同意才可以添加好友 manual
         * 添加直接通过accept_all
         */
        fun subscriptionMode(subscriptionMode: Roster.SubscriptionMode): Builder {
            config.subscriptionMode = subscriptionMode
            return this
        }

        /**
         * 设置答复超时
         */
        fun replyTimeout(replyTimeout: Int): Builder {
            config.replyTimeout = replyTimeout
            return this
        }

        /**
         * 设置答复超时
         */
        fun fromMode(fromMode: XMPPConnection.FromMode): Builder {
            config.fromMode = fromMode
            return this
        }

        fun isReconnection(isReconnection: Boolean): Builder {
            config.isReconnection = isReconnection
            return this
        }

        /**
         * 重新连接时间间隔，这个是初始化时间，内部会重连时候如果还是失败会每次增加
         * 如果关闭重新连接这里设置0
         */
        fun reconnectionTime(reconnectionTime: Int): Builder {
            config.reconnectionTime = reconnectionTime
            return this
        }

        fun pingInterval(pingInterval: Int): Builder {
            config.pingInterval = pingInterval
            return this
        }

        fun apply(): Boolean {
            if (TextUtils.isEmpty(config.host)) {
                return false
            } else {
                //初始化XMPPTCPConnection相关配置
                val builder = XMPPTCPConnectionConfiguration.builder()
                try {
                    //设置domain
                    builder.setXmppDomain(if (config.domain == null) config.host else config.domain)
                    //设置端名称
                    builder.setResource(config.resource)
                } catch (e: XmppStringprepException) {
                    e.printStackTrace()
                }
                //设置主机地址
                builder.setHost(config.host)
                //设置端口号
                builder.setPort(config.port)
                //设置连接超时的最大时间
                builder.setConnectTimeout(config.connectTimeout)
                //设置安全模式
                builder.setSecurityMode(config.securityMode)
                //是否开启压缩
                builder.setCompressionEnabled(config.compressionEnabled)
                //设置登录状态,不想登录传false
                builder.setSendPresence(config.sendPresence)
                //设置调试日志
                if (config.isDebug) {
                    builder.enableDefaultDebugger()
                }
                builder.allowEmptyOrNullUsernames()
                //需要经过同意才可以添加好友 manual 添加直接通过accept_all
                Roster.setDefaultSubscriptionMode(config.subscriptionMode)
                //重新连接机制
                EasyReconnectionManager.setDefaultReconnectionEnable(config.isReconnection)
                EasyReconnectionManager.setDefaultFixedDelay(config.reconnectionTime)
                EasyReconnectionManager.setDefaultReconnectionPolicy(EasyReconnectionManager.ReconnectionPolicy.FIXED_DELAY)
                //消息回执
                DeliveryReceiptManager.setDefaultAutoReceiptMode(config.receipt)
                //心跳包间隔
                PingManager.setDefaultPingInterval(config.pingInterval / 1000)
                XmppManage.get().init(builder.build(), config)
                return true
            }
        }

        companion object {

            fun create(application: Application): Builder {
                return Builder(application)
            }
        }
    }


}
