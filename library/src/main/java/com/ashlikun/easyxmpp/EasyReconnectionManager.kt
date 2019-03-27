package com.ashlikun.easyxmpp

import android.accounts.NetworkErrorException
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

    var disposable: Disposable? = null


    /**
     * 连接监听
     */
    private val connectionListener = object : AbstractConnectionListener() {
        override fun connectionClosed() {
        }

        override fun authenticated(connection: XMPPConnection?, resumed: Boolean) {
        }

        override fun connectionClosedOnError(e: Exception) {
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
            reconnect()
        } else {
            isReconnectUnavailable = true
        }
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
                //突然有网了得回掉时间为0
                XmppUtils.runMain {
                    for (listener in reconnectionListeners) {
                        listener.reconnectionTime(0)
                    }
                }
            }
            if (currentNetwork) {
                val connection = weakRefConnection.get() ?: return@Consumer
                XmppUtils.loge("重连中 isConnected = ${connection.isConnected}     isAuthenticated = ${connection.isAuthenticated}")
                if (!connection.isConnected) {
                    connection.connect()
                }
                if (!connection.isAuthenticated && XmppManage.getCM().userData.isValid()) {
                    connection.login()
                    //上线
                    XmppManage.getCM().userData.updateStateToAvailable()
                }
                disposable?.dispose()
            } else {
                //没有网络继续走
                throw NetworkErrorException("no network")
            }
        } else {
            //时间没到    回调
            XmppUtils.loge("重连倒计时 ${delayTime - it}")
            if (!reconnectionListeners.isEmpty()) {
                XmppUtils.runMain {
                    for (listener in reconnectionListeners) {
                        listener.reconnectionTime((delayTime - it).toInt())
                    }
                }
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
        return (isAutomaticReconnectEnabled)
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
        //是否正在运行重连  isDisposed = true 就是结束了
        if (disposable?.isDisposed != false && isReconnectionPossible()) {
            attempts = 0
            //延时时间间隔秒
            delayTime = timeDelay()
            isNetwork = XmppUtils.isNetworkConnected()
            //执行任务,第一个先立马执行
            Observable.just((delayTime + 1).toLong())
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
            ReconnectionManager.ReconnectionPolicy.FIXED_DELAY -> fixedDelay / 1000
            ReconnectionPolicy.RANDOM_INCREASING_DELAY ->
                when {
                    attempts > 13 -> // between 2.5 and 7.5 minutes (~5 minutes)
                        randomBase * 6 * 5
                    attempts > 7 -> // between 30 and 90 seconds (~1 minutes)
                        randomBase * 6
                    else -> // 10 seconds
                        randomBase
                }
            else -> randomBase
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

