package com.ashlikun.easyxmpp

import org.jivesoftware.smack.SASLAuthentication
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.StanzaError
import org.jivesoftware.smack.packet.StreamError
import org.jivesoftware.smack.sasl.SASLErrorException
import java.lang.reflect.Field


/**
 * 作者　　: 李坤
 * 创建时间: 2018/12/14　11:02
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：统一封装错误
 */
class SmackInvocationException constructor(throwable: Throwable?, detailMessage: String = "") : Exception(detailMessage, throwable) {
    constructor(detailMessage: String) : this(null, detailMessage)

    /**
     * 是否是认证错误
     * 1:账号或者密码不正确
     */
    fun isSASLError(): Boolean {
        if (cause is SASLErrorException) {
            cleanSASLError()
            return true
        }
        return false
    }

    /**
     * 是否没有连接服务器错误
     */
    fun isNotConnected() = cause is SmackException.NotConnectedException

    /**
     * 是否这个错误是在其他设备上登录了
     * 统一账户同一resource才会有这个错误
     */
    fun isLoginConflict(): Boolean {
        if (cause is XMPPException.StreamErrorException) {
            //在其他设备上登录了
            if (StreamError.Condition.conflict == cause.streamError?.condition) {
                return true
            }
        }
        return false
    }

    /**
     * 是否是XMPPErrorException 表示XMPP节点错误
     * 指定的错误
     */
    fun isStanzaError(condition: StanzaError.Condition): Boolean {
        if (cause is XMPPException.XMPPErrorException) {
            if (condition == cause.stanzaError?.condition) {
                return true
            }
        }
        return false
    }

    /**
     * 是否是StreamErrorException表示流错误
     * 指定的错误
     */
    fun isStreamError(condition: StreamError.Condition): Boolean {
        if (cause is XMPPException.StreamErrorException) {
            if (condition == cause.streamError?.condition) {
                return true
            }
        }
        return false
    }

    /**
     * 这个错误是否可以再次重新连接
     * @return true:可以重写连接，请重新连接
     */
    fun isErrorCanReconnect(): Boolean {
        if (cause is SmackException.AlreadyConnectedException) {
            //已经连接
            return false
        } else if (isLoginConflict()) {
            //在其他设备上登录了
            return false
        }
        return true
    }

    /**
     * 清空SASL错误时候 的异常
     * 因为错误后再次登录永远都是错误
     */
    fun cleanSASLError() {
        try {
            //反射获取saslAuthentication字段

            var field = getDeclaredField(XmppManage.getCM().connection.javaClass, "saslAuthentication")
            field?.isAccessible = true
            var sasl = field?.get(XmppManage.getCM().connection) as SASLAuthentication?
            //反射调用init方法
            var method = sasl?.javaClass?.getDeclaredMethod("init")
            method?.invoke(sasl)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 循环向上转型, 获取对象的 DeclaredField
     *
     * @param fieldName : 父类中的属性名
     * @return 父类中的属性对象
     */
    fun getDeclaredField(cls: Class<*>, fieldName: String): Field? {
        var field: Field? = null
        var clazz = cls
        while (clazz != Any::class.java) {
            try {
                field = clazz.getDeclaredField(fieldName)
                return field
            } catch (e: Exception) {
            }

            clazz = clazz.superclass
        }
        return null
    }
}