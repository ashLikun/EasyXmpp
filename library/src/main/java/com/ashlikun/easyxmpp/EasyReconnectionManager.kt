package com.ashlikun.easyxmpp

import android.util.Log
import com.ashlikun.easyxmpp.listener.EasyReconnectionListener
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.jivesoftware.smack.*
import org.jivesoftware.smack.filter.PresenceTypeFilter
import org.jivesoftware.smackx.ping.PingFailedListener
import org.jivesoftware.smackx.ping.PingManager
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

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

    internal var done = false
    private var attempts = 0
    /**
     * 延时时间间隔秒
     */
    private var delayTime = 0

    /**
     * 重连前是否有网
     */
    private var isNetwork = true

    var disposable: Disposable? = null


    /**
     * 连接监听
     */
    private val connectionListener = object : AbstractConnectionListener() {
        override fun connectionClosed() {
            done = true
        }

        override fun authenticated(connection: XMPPConnection?, resumed: Boolean) {
            done = false
        }

        override fun connectionClosedOnError(e: Exception?) {
            done = false
            if (!isAutomaticReconnectEnabled) {
                return
            }
            reconnect()
        }
    }
    /**
     * 离线监听服务器叫我离线的事件,实现永不离线
     */
    private val offlineListener = StanzaListener {
        //离线了,重新上线
        XmppUtils.loge("当前用户离线了" + it.toString())
        reconnect()
    }
    /**
     * 心跳包失败后再次启动
     */
    private val pingFailedListener = PingFailedListener {
        //心跳包失败,重新上线
        XmppUtils.loge("pingFailed当前用户离线了${XmppManage.getCM().userData}")
        reconnect()
    }

    init {
        if (isAutomaticReconnectEnabled) {
            //接收到服务器通知我离线的监听
            connection.addAsyncStanzaListener(offlineListener, PresenceTypeFilter.UNAVAILABLE)
            //心跳包失败后再次启动
            PingManager.getInstanceFor(connection).registerPingFailedListener(pingFailedListener)
            connection.addConnectionListener(connectionListener)
        }
    }

    /**
     *重新连接的回调
     */
    private val consumer = Consumer<Long> {
        //到达指定时间，或者网络变化，那么久去连接
        var currentNetwork = XmppUtils.isNetworkConnected()

        if (it > delayTime || (!isNetwork && currentNetwork)) {
            if (!isNetwork && currentNetwork) {
                isNetwork = currentNetwork
            }
            val connection = weakRefConnection.get() ?: return@Consumer
            if (connection.isConnected) {
                connection.disconnect()
            }
            connection.connect()
            //如果有登录信息去登录
            XmppManage.getCM().userData?.neibuLogin()
            disposable?.dispose()
        } else {
            //时间没到    回调
            Log.e("aaaaa", "${delayTime - it}")
            if (!reconnectionListeners.isEmpty()) {
                XmppUtils.runMain(Consumer {
                    for (listener in reconnectionListeners) {
                        listener.reconnectionTime(delayTime - it)
                    }
                })
            }
        }
    }
    /**
     * 重新连接异常
     */
    private val consumerError = Consumer<Throwable> { t ->
        // SmackException.AlreadyLoggedInException已经登录了,重连结束
        if (t !is SmackException.AlreadyLoggedInException) {
            if (XmppManage.get().config.isDebug) {
                XmppUtils.loge(t.toString())
            }
            //回调
            for (listener in reconnectionListeners) {
                listener.reconnectionFailed(t)
            }
            reStart()
        } else {
            disposable?.dispose()
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
        if (isReconnectionPossible()) {
            //延时时间间隔秒
            delayTime = timeDelay()
            if (disposable?.isDisposed == false) {
                disposable?.dispose()
            }
            disposable = Observable.interval(0, 1, TimeUnit.SECONDS)
                    .observeOn(Schedulers.newThread())
                    .subscribe(consumer, consumerError)
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
     * 是否可以重连
     */
    private fun isReconnectionPossible(): Boolean {
        return (!done && isAutomaticReconnectEnabled)
    }

    /**
     * 重新连接
     */
    @Synchronized
    fun reconnect() {
        val connection = this.weakRefConnection.get()
        if (connection == null) {
            LOGGER.fine("Connection is null, will not reconnect")
            return
        }
        //是否正在运行重连  isDisposed = true 就是结束了
        if (disposable?.isDisposed != false && isReconnectionPossible()) {
            attempts = 0
            //延时时间间隔秒
            delayTime = timeDelay()
            isNetwork = XmppUtils.isNetworkConnected()
            //执行任务
            disposable = Observable.interval(0, 1, TimeUnit.SECONDS)
                    .observeOn(Schedulers.newThread())
                    .subscribe(consumer, consumerError)
        }
    }

    /**
     * 取消定时任务
     */
    @Synchronized
    fun cancel() {
        if (disposable?.isDisposed == false) {
            disposable?.dispose()
        }
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
        private val LOGGER = Logger.getLogger(EasyReconnectionManager::class.java.name)

        private val INSTANCES = WeakHashMap<AbstractXMPPConnection, EasyReconnectionManager>()

        @Synchronized
        fun getInstanceFor(connection: AbstractXMPPConnection): EasyReconnectionManager {
            var reconnectionManager: EasyReconnectionManager? = INSTANCES[connection]
            if (reconnectionManager == null) {
                reconnectionManager = EasyReconnectionManager(connection)
                INSTANCES[connection] = reconnectionManager
            }
            return reconnectionManager
        }

        init {
            XMPPConnectionRegistry.addConnectionCreationListener { connection ->
                if (connection is AbstractXMPPConnection) {
                    ReconnectionManager.getInstanceFor(connection)
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
