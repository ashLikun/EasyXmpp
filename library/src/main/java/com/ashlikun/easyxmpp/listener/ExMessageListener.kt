package com.ashlikun.easyxmpp.listener

import com.ashlikun.easyxmpp.EXmppChatManage
import com.ashlikun.easyxmpp.EXmppUtils
import com.ashlikun.easyxmpp.data.ChatMessage
import com.ashlikun.easyxmpp.status.MessageStatus
import io.reactivex.functions.Consumer
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener
import org.jivesoftware.smack.packet.Message
import org.jxmpp.jid.EntityBareJid
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/30　10:23
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：发送与接受消息的监听
 */
class ExMessageListener : IncomingChatMessageListener, OutgoingChatMessageListener {
    /**
     * 这2个是切换主线程的回调
     */
    private val incomingListeners = CopyOnWriteArraySet<IncomingChatMessageListener>()
    private val outgoingListeners = CopyOnWriteArraySet<OutgoingChatMessageListener>()

    init {
        addIncomingListenerSubThread(this)
        addOutgoingListenerSubThread(this)
    }

    /**
     * 添加接受消息监听
     * 回调在子线程，XMPP默认的
     *
     * @param listener
     */
    fun addIncomingListenerSubThread(listener: IncomingChatMessageListener) {
        EXmppChatManage.getCm().removeIncomingListener(listener)
        EXmppChatManage.getCm().addIncomingListener(listener)
    }

    fun removeIncomingListenerSubThread(listener: IncomingChatMessageListener) {
        EXmppChatManage.getCm().removeIncomingListener(listener)
    }

    /**
     * 添加发出消息监听
     * 回调在子线程，XMPP默认的
     *
     * @param listener
     */
    fun addOutgoingListenerSubThread(listener: OutgoingChatMessageListener) {
        EXmppChatManage.getCm().removeOutgoingListener(listener)
        EXmppChatManage.getCm().addOutgoingListener(listener)
    }

    fun removeOutgoingListenerSubThread(listener: OutgoingChatMessageListener) {
        EXmppChatManage.getCm().removeOutgoingListener(listener)
    }

    /**
     * 添加接受消息监听
     * 回调在主线程
     *
     * @param listener
     */
    fun addIncomingListener(listener: IncomingChatMessageListener) {
        if (!incomingListeners.contains(listener)) {
            incomingListeners.add(listener)
        }
    }

    fun removeIncomingListener(listener: IncomingChatMessageListener) {
        if (incomingListeners.contains(listener)) {
            incomingListeners.remove(listener)
        }
    }

    /**
     * 添加发出消息监听
     * 回调在主线程
     *
     * @param listener
     */
    fun addOutgoingListener(listener: OutgoingChatMessageListener) {
        if (!outgoingListeners.contains(listener)) {
            outgoingListeners.add(listener)
        }
    }

    fun removeOutgoingListener(listener: OutgoingChatMessageListener) {
        if (outgoingListeners.contains(listener)) {
            outgoingListeners.remove(listener)
        }
    }


    /**
     * 接收到消息
     */
    override fun newIncomingMessage(from: EntityBareJid, message: Message, chat: Chat) {
        //保存消息到本地
        ChatMessage.getMyAcceptMessage(message).save()
        //回调主线程
        EXmppUtils.runMain(Consumer {
            for (listener in incomingListeners) {
                listener.newIncomingMessage(from, message, chat)
            }
        })
    }

    /**
     * 自己发送的消息
     */
    override fun newOutgoingMessage(to: EntityBareJid, message: Message, chat: Chat) {
        //改变数据库消息状态
        ChatMessage.changMessageStatus(message, MessageStatus.SUCCESS)
        //回调主线程
        EXmppUtils.runMain(Consumer {
            for (listener in outgoingListeners) {
                listener.newOutgoingMessage(to, message, chat)
            }
        })
    }
}