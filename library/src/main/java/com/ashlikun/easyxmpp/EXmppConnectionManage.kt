package com.ashlikun.easyxmpp

import android.text.TextUtils
import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jivesoftware.smack.AbstractConnectionListener
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　16:37
 * 邮箱　　：496546144@qq.com
 *
 *
 * 功能介绍：连接管理器,对应连接的一些操作
 * 连接服务器，登录
 */
class EXmppConnectionManage internal constructor(
        /**
         * 整个Xmpp  tcp连接
         */
        var connection: XMPPTCPConnection) : AbstractConnectionListener() {

    var callbackList: MutableList<ConnectionCallback> = ArrayList()
    /**
     * 失败累计计数
     */
    private var count = 1f
    /**
     * 用户名与密码
     */
    private var userName: String = ""
    /**
     * 获取密码
     */
    private var password: String = ""

    private val time: Int
        get() = (EXmppManage.get().config.reconnectionTime * count).toInt()

    /**
     * 是否登录
     *
     * @return
     */
    val isAuthenticated: Boolean
        get() = connection.isAuthenticated

    /**
     * 是否连接
     *
     * @return
     */
    val isConnected: Boolean
        get() = connection.isConnected

    val domain: String
        get() = connection.xmppServiceDomain.domain.toString()

    /**
     * 连接服务器
     * 未处理异常
     */
    @Throws(Exception::class)
    internal fun connectSub() {
        if (!EXmppManage.isConnected()) {
            connection.connect()
        }
    }

    /**
     * 连接服务器
     * 处理异常
     */
    fun connect() {
        Observable.just(1).observeOn(Schedulers.newThread())
                .subscribe({
                    if (!connection.isConnected) {
                        connection.connect()
                    }
                }, {
                    it.printStackTrace()
                    reconnection()
                })
    }

    private fun reconnection() {
        if (!EXmppManage.get().config.isReconnection) {
            return
        }
        count = 1f
        start()
    }

    private fun start() {
        Observable.timer(time.toLong(), TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe({
                    if (!EXmppManage.isConnected()) {
                        connectSub()
                    }
                }, {
                    //异常了继续重新连接
                    count++
                    start()
                })
    }

    /**
     * 登录
     */
    fun login(callback: LoginCallback?) {
        login(userName, password, callback)
    }

    /**
     * 登录,如果失败在重新连接的时候会登录
     */
    fun login(userName: String, password: String, callback: LoginCallback?) {
        this.userName = userName
        this.password = password
        Observable.just(1)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    connection.login(userName, password)
                    connection.isAuthenticated
                }
                .subscribe({
                    if (it) {
                        callback?.loginSuccess(userName, password)
                    } else {
                        callback?.loginError(userName, password, Exception("isAuthenticated == false"))
                    }
                }, { throwable ->
                    //登录失败，回调
                    callback?.loginError(userName, password, throwable)
                })
    }

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

    fun setUserName(userName: String, password: String) {
        this.userName = userName
        this.password = password
    }

    override fun connected(connection: XMPPConnection) {
        super.connected(connection)
        if (EXmppManage.get().config.isDebug) {
            Log.e(TAG, "连接成功")
        }
        //如果有登录信息会再次登录
        if (!TextUtils.isEmpty(userName) && !TextUtils.isEmpty(password)) {
            if (EXmppManage.isConnected() && !EXmppManage.isAuthenticated()) {
                login(null)
            }
        }
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
        if (EXmppManage.get().config.isDebug) {
            Log.e(TAG, "authenticated$resumed")
        }
    }

    /**
     * 连接关闭
     */
    override fun connectionClosed() {
        super.connectionClosed()
        if (EXmppManage.get().config.isDebug) {
            Log.e(TAG, "connectionClosed")
        }
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
        if (EXmppManage.get().config.isDebug) {
            Log.e(TAG, "connectionClosedOnError" + e.toString())
        }
        for (callback in callbackList) {
            callback.connectionError(e)
        }
    }

    companion object {
        val TAG = "EXmppConnectionManage"
    }
}
