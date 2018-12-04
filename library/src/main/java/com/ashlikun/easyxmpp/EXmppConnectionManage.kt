package com.ashlikun.easyxmpp

import com.ashlikun.easyxmpp.data.User
import com.ashlikun.easyxmpp.listener.ConnectionCallback
import com.ashlikun.easyxmpp.listener.ExConnectionListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.tcp.XMPPTCPConnection


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
     * 用户信息
     */
    var userData: User = User()
        private set


    init {
        connection.addConnectionListener(callbackListener)

    }

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
                    XmppManage.getRM().reconnect()
                })
    }

    /**
     * 断开连接
     * 处理异常
     */
    fun disconnect() {
        Observable.just(1).observeOn(Schedulers.newThread())
                .subscribe({
                    //取消重连任务
                    XmppManage.getRM().cancel()
                    if (connection.isConnected) {
                        connection.disconnect()
                    }
                }, {
                    it.printStackTrace()
                })
    }

    /**
     * 登录
     */
    fun login(bolock: (user: User, isSuccess: Boolean, throwable: Throwable?) -> Unit) {
        userData.login(bolock)
    }

    /**
     * 登录,请在失败的时候自己处理
     */
    fun login(userName: String, password: String, bolock: (user: User, isSuccess: Boolean, throwable: Throwable?) -> Unit) {
        userData = User(userName, password)
        userData.login(bolock)
    }

    /**
     * 发送一个数据
     *@param bolock 回调方法
     */
    fun sendStanza(stanza: Stanza, bolock: (iSsuccess: Boolean) -> Unit) {
        Observable.create<Boolean> {
            try {
                if (connection.isConnected) {
                    connection.sendStanza(stanza)
                    true
                } else false
            } catch (e: Exception) {
                false
            }
        }.observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(Consumer(bolock))
    }

    /**
     * 添加连接监听
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
