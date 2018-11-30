package com.ashlikun.easyxmpp.data

import com.ashlikun.easyxmpp.EXmppManage
import org.jxmpp.jid.EntityFullJid

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
     * 获取xmpp user对象
     */
    fun getUserJid(): EntityFullJid {
        return EXmppManage.getCM().connection.user
    }

    /**
     * 获取xmpp user对象 String
     */
    fun getUser(): String {
        return getUserJid().toString()
    }
}
