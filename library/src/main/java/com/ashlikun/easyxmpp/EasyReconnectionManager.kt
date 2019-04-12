package com.ashlikun.easyxmpp

import android.accounts.NetworkErrorException
import com.ashlikun.easyxmpp.listener.EasyReconnectionListener
import io.reactivex.functions.Consumer
import org.jivesoftware.smack.*
import org.jivesoftware.smack.filter.AndFilter
import org.jivesoftware.smack.filter.PresenceTypeFilter
import org.jivesoftware.smack.filter.ToMatchesFilter
import org.jivesoftware.smack.sasl.provided.SASLPlainMechanism
import org.jivesoftware.smack.util.Async
import org.jivesoftware.smackx.ping.PingFailedListener
import org.jivesoftware.smackx.ping.PingManager
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

/**
 * @author　　: 李坤
 * 创建时间: 2018/12/2 17:50
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：重连机制
 */
class EasyReconnectionManager private constructor(connection: AbstractXMPPConnection) {
    /**
     * 重连监听
     */
    private val reconnectionListeners = CopyOnWriteArraySet<EasyReconnectionListener>()

    private val weakRefConnection: WeakReference<AbstractXMPPConnection> = WeakReference(connection)
    private val randomBase = Random().nextInt(13) + 2 // between 2 and 15 seconds

    @Volatile
    private var fixedDelay = defaultFixedDelay
    /**
     * 重连时间的间隔
     */
    @Volatile
    private var reconnectionPolicy = defaultReconnectionPolicy
    /**
     * 是否自动重连
     */
    var isAutomaticReconnectEnabled = EasyReconnectionManager.defaultAutomaticReconnectEnabled
        private set
        @Synchronized
        get
    /**
     * 离线重连是否关闭
     * true:开启
     * false:暂时关闭关闭，关闭的时候在回掉后会设置成开启，也就是一次机会
     */
    var isReconnectUnavailable = true
        @Synchronized
        get


    private var attempts = 0
    /**
     * 延时时间间隔秒
     */
    private var delayTime = 0

    /**
     * 重连前是否有网
     */
    private var isNetwork = true

    var thread: Thread? = null
    private var runnable: MyRunnable? = null

    /**
     * 连接监听
     */
    private val connectionListener = object : AbstractConnectionListener() {
        override fun connectionClosed() {
            XmppManage.getCM().connection.disconnect()
            //销毁定时器
            thread = null
        }

        override fun authenticated(connection: XMPPConnection?, resumed: Boolean) {
        }

        override fun connectionClosedOnError(e: Exception) {
            //销毁定时器
            thread = null
            XmppManage.getCM().connection.disconnect()
            SASLAuthentication.registerSASLMechanism( SASLPlainMechanism())
            var e2 = SmackInvocationException(e)
            if (e2.isErrorCanReconnect()) {
                reconnect()
            }
        }
    }
    /**
     * 离线监听服务器叫我离线的事件,实现永不离线
     */
    private val offlineListener = StanzaListener {
        //离线了,重新上线
        if (isReconnectUnavailable) {
            XmppUtils.loge("当前用户离线了" + it.toString())
            XmppManage.getCM().instantShutdown()
            reconnect()
        } else {
            isReconnectUnavailable = true
            //更新为上线
            XmppManage.getCM().userData.updateStateToAvailable()
            val connection = weakRefConnection.get()
            if (connection != null) {
                XmppUtils.runNew { PingManager.getInstanceFor(connection).pingServerIfNecessary() }
            }
        }
    }
    /**
     * 心跳包失败后再次启动
     */
    private val pingFailedListener = PingFailedListener {
        //心跳包失败,重新上线
        if (isReconnectUnavailable) {
            XmppUtils.loge("pingFailed当前用户离线了${XmppManage.getCM().userData}")
            XmppManage.getCM().instantShutdown()
            reconnect()
        } else {
            isReconnectUnavailable = true
        }
        reconnect()
    }

    init {
        if (isAutomaticReconnectEnabled) {
            //接收到服务器通知我离线的监听
            connection.addAsyncStanzaListener(offlineListener, AndFilter(PresenceTypeFilter.UNAVAILABLE, ToMatchesFilter.create(weakRefConnection?.get()?.user)))
            //心跳包失败后再次启动
            PingManager.getInstanceFor(connection).registerPingFailedListener(pingFailedListener)
            connection.addConnectionListener(connectionListener)
        }
    }

    /**
     * 重新连接的回调
     */
    private abstract class MyRunnable : Runnable {
        var isCancel = false
    }

    /**
     * 重新连接的回调
     */
    private fun createRunable() = object : MyRunnable() {
        override fun run() {
            //是否取消
            if (isCancel) {
                //销毁定时器
                thread = null
                return
            }
            try {
                //延时时间间隔秒
                delayTime = timeDelay()
                //当前定时到的时间
                var currentTime = 0
                //到达指定时间，或者网络变化，那么久去连接
                //循环定时
                while (!(currentTime > delayTime || (!isNetwork && XmppUtils.isNetworkConnected()))) {
                    //是否取消
                    if (isCancel) {
                        //销毁定时器
                        thread = null
                        return
                    }
                    //休息1S
                    Thread.sleep(1000)
                    //时间没到 增加时间
                    XmppUtils.loge("重连倒计时 ${delayTime - currentTime}")
                    if (!reconnectionListeners.isEmpty()) {
                        XmppUtils.runMain {
                            reconnectionListeners.forEach { it.reconnectionTime((delayTime - currentTime)) }
                        }
                    }
                    currentTime++
                }
                if (!isNetwork && XmppUtils.isNetworkConnected()) {
                    currentTime = delayTime
                    isNetwork = XmppUtils.isNetworkConnected()
                    //突然有网了得回掉时间为0
                    XmppUtils.loge("重连倒计时 ${delayTime - currentTime}")
                    XmppUtils.runMain {
                        reconnectionListeners.forEach { it.reconnectionTime(0) }
                    }
                }
                if (XmppUtils.isNetworkConnected()) {
                    if (weakRefConnection.get() == null) {
                        //销毁定时器
                        thread = null
                    }
                    val connection = weakRefConnection.get() ?: return
                    XmppUtils.loge("重连中 isConnected = ${connection.isConnected}     isAuthenticated = ${connection.isAuthenticated}")
                    if (!connection.isConnected) {
                        connection.connect()
                    }
                    if (!connection.isAuthenticated && XmppManage.getCM().userData.isValid()) {
                        //登录
                        try {
                            connection.login()
                        } catch (e: SmackException.AlreadyLoggedInException) {
                            //已经登录的异常过滤
                        }
                        //上线
                        XmppManage.getCM().userData.updateStateToAvailable()
                    }
                    //销毁定时器
                    thread = null
                } else {
                    //没有网络继续走
                    throw NetworkErrorException("no network")
                }
            } catch (e: Throwable) {
                //错误
                consumerError.accept(e)
            }
        }

    }


    /**
     * 重新连接异常
     */
    private val consumerError = Consumer<Throwable> { t ->
        // SmackException.AlreadyLoggedInException已经登录了,重连结束
        var exception = SmackInvocationException(t)
        if (!exception.isLoginConflict()) {
            if (XmppManage.get().config.isDebug) {
                XmppUtils.loge("重新连接异常  $t")
            }
            XmppUtils.runMain {
                reconnectionListeners.forEach { it.reconnectionFailed(t) }
            }
            reStart()
        } else {
            thread = null
        }
    }

    /**
     * 重连监听
     */
    fun addReconnectionListener(listener: EasyReconnectionListener): Boolean {
        return if (reconnectionListeners.contains(listener)) false else reconnectionListeners.add(listener)
    }

    fun removeReconnectionListener(listener: EasyReconnectionListener): Boolean {
        return reconnectionListeners.remove(listener)
    }

    /**
     * 设置时间间隔
     */
    fun setFixedDelay(fixedDelay: Int) {
        this.fixedDelay = fixedDelay
        setReconnectionPolicy(EasyReconnectionManager.ReconnectionPolicy.FIXED_DELAY)
    }

    /**
     * 时间间隔是怎么实现的
     */
    fun setReconnectionPolicy(reconnectionPolicy: EasyReconnectionManager.ReconnectionPolicy) {
        this.reconnectionPolicy = reconnectionPolicy
    }


    private fun reStart() {
        //再次执行任务
        if (isAutomaticReconnectEnabled) {
            XmppUtils.loge("重新启动重连定时器 ")
            attempts = 0
            isNetwork = XmppUtils.isNetworkConnected()
            //执行任务
            if (runnable == null) {
                runnable?.isCancel = true
                runnable = createRunable()
            }
            runnable?.run()
        }
    }

    /**
     * 关闭自动连接
     */
    @Synchronized
    private fun disableAutomaticReconnection() {
        if (!isAutomaticReconnectEnabled) {
            return
        }
        val connection = weakRefConnection.get()
        connection?.removeConnectionListener(connectionListener)
    }


    /**
     * 重新连接,并且重新登录
     * 请判断异常后再调用
     */
    @Synchronized
    fun reconnect() {
        if (!isAutomaticReconnectEnabled) {
            return
        }
        val connection = this.weakRefConnection.get()
        if (connection == null) {
            XmppUtils.loge("Connection is null, will not reconnect")
            return
        }
        //是否正在运行重连
        if (thread == null && isAutomaticReconnectEnabled) {
            XmppUtils.loge("启动重连定时器 ")
            attempts = 0
            isNetwork = XmppUtils.isNetworkConnected()
            //执行任务,第一个先立马执行
            runnable?.isCancel = true
            runnable = createRunable()
            thread = Async.go(runnable)
        }
    }

    /**
     * 取消定时任务
     */
    @Synchronized
    fun cancel() {
        runnable?.isCancel = true
    }


    /**
     * 计算延时时间
     */
    private fun timeDelay(): Int {
        attempts++
        return when (reconnectionPolicy) {
            ReconnectionPolicy.FIXED_DELAY -> fixedDelay / 1000
            ReconnectionPolicy.RANDOM_INCREASING_DELAY ->
                when {
                    attempts > 13 -> // between 2.5 and 7.5 minutes (~5 minutes)
                        randomBase * 6 * 5
                    attempts > 7 -> // between 30 and 90 seconds (~1 minutes)
                        randomBase * 6
                    else -> // 10 seconds
                        randomBase
                }
        }
    }

    enum class ReconnectionPolicy {
        RANDOM_INCREASING_DELAY,
        FIXED_DELAY
    }

    companion object {
        private val INSTANCES = WeakHashMap<AbstractXMPPConnection, EasyReconnectionManager>()

        @Synchronized
        fun getInstanceFor(connection: AbstractXMPPConnection): EasyReconnectionManager {
            var reconnectionManager: EasyReconnectionManager = INSTANCES[connection]
                    ?: EasyReconnectionManager(connection)
            INSTANCES[connection] = reconnectionManager
            return reconnectionManager
        }

        init {
            XMPPConnectionRegistry.addConnectionCreationListener { connection ->
                if (connection is AbstractXMPPConnection) {
                    EasyReconnectionManager.getInstanceFor(connection)
                }
            }
        }

        private var defaultFixedDelay = 15000
        private var defaultAutomaticReconnectEnabled = true
        private var defaultReconnectionPolicy = ReconnectionPolicy.RANDOM_INCREASING_DELAY

        fun setDefaultFixedDelay(fixedDelay: Int) {
            defaultFixedDelay = fixedDelay
            setDefaultReconnectionPolicy(ReconnectionPolicy.FIXED_DELAY)
        }

        fun setDefaultReconnectionPolicy(reconnectionPolicy: ReconnectionPolicy) {
            defaultReconnectionPolicy = reconnectionPolicy
        }

        fun setDefaultReconnectionEnable(defaultAutomatic: Boolean) {
            defaultAutomaticReconnectEnabled = defaultAutomatic
        }
    }
}

