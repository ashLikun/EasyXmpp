package com.ashlikun.easyxmpp

import com.ashlikun.easyxmpp.data.ChatMessage
import com.ashlikun.easyxmpp.status.MessageStatus
import io.reactivex.functions.Consumer
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener
import org.jivesoftware.smack.packet.Message
import org.jxmpp.jid.EntityBareJid
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.stringprep.XmppStringprepException
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　15:44
 * 邮箱　　：496546144@qq.com
 *
 *
 * 功能介绍：聊天管理器
 * 对应消息的操作都在这
 */
class EXmppChatManage private constructor() : IncomingChatMessageListener, OutgoingChatMessageListener {
    companion object {
        private val instance by lazy { EXmppChatManage() }
        fun get(): EXmppChatManage = instance
    }

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
     * 获取一个Chat
     */
    fun getChat(name: String): Chat? {
        try {
            return EXmppManage.get().chatManager.chatWith(JidCreate.entityBareFrom(EXmppUtils.getJidName(name)))
        } catch (e: XmppStringprepException) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * 发送一条消息给Chat
     */
    fun sendMessage(name: String, content: String): Boolean {
        val stanza = Message()
        stanza.body = content
        stanza.type = Message.Type.chat
        return sendMessage(name, stanza)
    }

    /**
     * 发送一条消息给Chat
     */
    fun sendMessage(name: String, content: Message): Boolean {
        return if (content == null) false else
            try {
                //先保存数据库
                var chatMessage = ChatMessage(content.stanzaId,
                        MessageStatus.SENDING,
                        content.body,
                        name,
                        )
                val chat = getChat(name)
                chat?.send(content)
                chat != null
            } catch (e: SmackException.NotConnectedException) {
                e.printStackTrace()
                false
            } catch (e: InterruptedException) {
                e.printStackTrace()
                false
            }

    }

    /**
     * 添加接受消息监听
     * 回调在子线程，XMPP默认的
     *
     * @param listener
     */
    fun addIncomingListenerSubThread(listener: IncomingChatMessageListener) {
        EXmppManage.get().chatManager.removeIncomingListener(listener)
        EXmppManage.get().chatManager.addIncomingListener(listener)
    }

    fun removeIncomingListenerSubThread(listener: IncomingChatMessageListener) {
        EXmppManage.get().chatManager.removeIncomingListener(listener)
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
     * 回调在子线程，XMPP默认的
     *
     * @param listener
     */
    fun addOutgoingListenerSubThread(listener: OutgoingChatMessageListener) {
        EXmppManage.get().chatManager.removeOutgoingListener(listener)
        EXmppManage.get().chatManager.addOutgoingListener(listener)
    }

    fun removeOutgoingListenerSubThread(listener: OutgoingChatMessageListener) {
        EXmppManage.get().chatManager.removeOutgoingListener(listener)
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

    override fun newIncomingMessage(from: EntityBareJid, message: Message, chat: Chat) {
        //回调主线程
        EXmppUtils.runMain(Consumer {
            for (listener in incomingListeners) {
                listener.newIncomingMessage(from, message, chat)
            }
        })
    }

    override fun newOutgoingMessage(to: EntityBareJid, message: Message, chat: Chat) {
        EXmppUtils.runMain(Consumer {
            for (listener in outgoingListeners) {
                listener.newOutgoingMessage(to, message, chat)
            }
        })
    }
}
