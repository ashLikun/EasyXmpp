package com.ashlikun.easyxmpp.data

import com.ashlikun.easyxmpp.XmppManage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.Presence
import java.io.IOException


/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/30　17:37
 * 邮箱　　：496546144@qq.com
 * 功能介绍：当前登录的用户信息
 */
data class User(
        /**
         * 用户名与密码
         */
        var userName: String = "",
        var password: String = "") {

    /**
     * 获取xmpp user对象 String
     */
    fun getUser(): String {
        if (XmppManage.getCM().connection.user == null) {
            return userName
        }
        return XmppManage.getCM().connection.user?.localpart.toString()
    }

    /**
     * 直接登录内部调用
     */
    @Throws(XMPPException::class, SmackException::class, IOException::class, InterruptedException::class)
    internal fun neibuLogin() {
        if (userName.isNotEmpty() && password.isNotEmpty()) {
            XmppManage.getCM().connection.login(userName, password)
        }
    }

    /**
     * 登录,请在失败的时候自己处理
     */
    fun login(bolock: (user: User, isSuccess: Boolean, throwable: Throwable?) -> Unit) {
        Observable.just(1)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    XmppManage.getCM().userData = this
                    XmppManage.getCM().connection.login(userName, password)
                    XmppManage.getCM().connection.isAuthenticated
                }.subscribe({

                    if (it) {
                        bolock(this, it, null)
                    } else {
                        bolock(this, it, Exception("isAuthenticated == false"))
                    }
                }, { throwable ->
                    //登录失败，回调
                    bolock(this, false, throwable)
                })
    }

    /**
     * 更新为上线
     */
    fun updateStateToAvailable(): Boolean {
        return updateState(Presence.Type.available)
    }

    /**
     * 更新用户状态,要在子线程执行
     */
    fun updateState(type: Presence.Type): Boolean {
        return if (XmppManage.isAuthenticated()) {
            try {
                XmppManage.getCM().connection.sendStanza(Presence(type))
                true
            } catch (e: Exception) {
            }
            false
        } else false
    }

}
