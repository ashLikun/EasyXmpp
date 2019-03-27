package com.ashlikun.easyxmpp

import com.ashlikun.easyxmpp.data.User
import com.ashlikun.easyxmpp.listener.ConnectionCallback
import com.ashlikun.easyxmpp.listener.ExConnectionListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
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
        internal set

    fun getUserName() = userData.getUser()


    init {
        connection.addConnectionListener(callbackListener)
    }

    /**
     * 是否正在连接服务器
     */
    var isConnectedIng = false
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
        get() = connection.user

    /**
     * 连接服务器
     * 处理异常
     */
    fun connect() {
        if (isConnectedIng) {
            return
        }
        XmppManage.getRM().cancel()
        isConnectedIng = true
        Observable.just(1).observeOn(Schedulers.newThread())
                .subscribe({
                    try {
                        if (!connection.isConnected) {
                            connection.connect()
                        }
                    } catch (e: Exception) {
                        throw SmackInvocationException(e)
                    }
                    isConnectedIng = false
                }, {
                    it.printStackTrace()
                    isConnectedIng = false
                    if (it is SmackInvocationException && it.isErrorCanReconnect()) {
                        XmppManage.getRM().reconnect()
                    }
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
     * 退出登录
     */
    fun logout() {
        cleanLogin()
        XmppManage.getCM().connection.disconnect()
        //保证服务器连接不断开，只退出登录
        XmppManage.getRM().reconnect()
    }

    fun instantShutdown() {
        XmppManage.getCM().connection?.instantShutdown()
    }

    /**
     * 清空登录信息
     */
    fun cleanLogin() {
        User().login { user, isSuccess, throwable -> }
    }

    /**
     * 登录
     */
    fun login(bolock: (user: User, isSuccess: Boolean, throwable: SmackInvocationException?) -> Unit) {
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
            it.onNext(try {
                if (connection.isConnected) {
                    connection.sendStanza(stanza)
                    true
                } else false
            } catch (e: Exception) {
                false
            })
        }.observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(bolock, {
                    bolock(false)
                })

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
