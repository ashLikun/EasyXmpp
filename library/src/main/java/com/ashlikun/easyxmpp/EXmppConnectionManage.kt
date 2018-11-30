package com.ashlikun.easyxmpp

import com.ashlikun.easyxmpp.data.User
import com.ashlikun.easyxmpp.listener.ConnectionCallback
import com.ashlikun.easyxmpp.listener.ExConnectionListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jivesoftware.smack.tcp.XMPPTCPConnection
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
class EXmppConnectionManage internal constructor(var connection: XMPPTCPConnection) {

    private var callbackListener = ExConnectionListener()
    /**
     * 失败累计计数
     */
    private var count = 1
    /**
     * 用户信息
     */
    var userData: User = User()
        private set


    init {
        connection.addConnectionListener(callbackListener)
    }


    private fun getTime(): Long = EXmppManage.get().config.reconnectionTime * count

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
        count = 1
        start()
    }

    private fun start() {
        Observable.timer(getTime(), TimeUnit.MILLISECONDS)
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
        userData.run {
            login(userName, password, callback)
        }
    }

    /**
     * 登录,请在失败的时候自己处理
     */
    fun login(userName: String, password: String, callback: LoginCallback?) {
        userData = User(userName, password)
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
        callbackListener.addCallback(callback)
    }

    fun removeCallback(callback: ConnectionCallback) {
        callbackListener.removeCallback(callback);
    }

}
