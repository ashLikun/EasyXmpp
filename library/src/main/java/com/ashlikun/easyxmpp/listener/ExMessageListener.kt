package com.ashlikun.easyxmpp.listener

import android.util.Log
import com.ashlikun.easyxmpp.XmppManage
import com.ashlikun.easyxmpp.XmppUtils
import com.ashlikun.easyxmpp.data.ChatMessage
import com.ashlikun.easyxmpp.status.MessageStatus
import io.reactivex.functions.Consumer
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.delay.DelayInformationManager
import org.jxmpp.jid.EntityBareJid
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/30　10:23
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：发送与接受消息的监听
 */
class ExMessageListener constructor(var chatManage: ChatManager) : IncomingChatMessageListener, OutgoingChatMessageListener {
    /**
     * 这2个是切换主线程的回调
     */
    private val receiveListeners = CopyOnWriteArraySet<ReceiveMessageListener>()
    private val sendListeners = CopyOnWriteArraySet<SendMessageListener>()

    /**
     * 是否删除过这个用户的离线消息
     */
    private val isOfflineDeleteMessage = HashMap<String, Boolean>()

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
        chatManage.removeIncomingListener(listener)
        chatManage.addIncomingListener(listener)
    }

    fun removeIncomingListenerSubThread(listener: IncomingChatMessageListener) {
        chatManage.removeIncomingListener(listener)
    }

    /**
     * 添加发出消息监听
     * 回调在子线程，XMPP默认的
     *
     * @param listener
     */
    fun addOutgoingListenerSubThread(listener: OutgoingChatMessageListener) {
        chatManage.removeOutgoingListener(listener)
        chatManage.addOutgoingListener(listener)
    }

    fun removeOutgoingListenerSubThread(listener: OutgoingChatMessageListener) {
        chatManage.removeOutgoingListener(listener)
    }

    /**
     * 添加接受消息监听
     * 回调在主线程
     *
     * @param listener
     */
    fun addReceiveListener(listener: ReceiveMessageListener) {
        if (!receiveListeners.contains(listener)) {
            receiveListeners.add(listener)
        }
    }

    fun removeReceiveListener(listener: ReceiveMessageListener) {
        if (receiveListeners.contains(listener)) {
            receiveListeners.remove(listener)
        }
    }

    /**
     * 添加发出消息监听
     * 回调在主线程
     *
     * @param listener
     */
    fun addSendListener(listener: SendMessageListener) {
        if (!sendListeners.contains(listener)) {
            sendListeners.add(listener)
        }
    }

    fun removeSendListener(listener: SendMessageListener) {
        if (sendListeners.contains(listener)) {
            sendListeners.remove(listener)
        }
    }


    /**
     * 接收到消息
     */
    override fun newIncomingMessage(from: EntityBareJid, message: Message, chat: Chat) {
        //保存消息到本地
        ChatMessage.getMyAcceptMessage(message).save()
        var chatMessage = ChatMessage.findMessageId(message.stanzaId)
        if (isOfflineDeleteMessage[XmppManage.getCM().userData.userName] != true && DelayInformationManager.isDelayedStanza(message)) {
            //通知服务器删除离线消息
            try {
                XmppManage.getOM().deleteMessages()
                isOfflineDeleteMessage[XmppManage.getCM().userData.userName] = true
            } catch (e: Exception) {
                if (XmppManage.get().config.isDebug) {
                    XmppUtils.loge("删除失败$e")
                }
            }
        }
        //回调主线程
        XmppUtils.runMain(Consumer {
            for (listener in receiveListeners) {
                listener.onReceiveMessage(from, message, chatMessage, chat)
            }
        })
    }

    /**
     * 自己发送的消息
     */
    override fun newOutgoingMessage(to: EntityBareJid, message: Message, chat: Chat) {
        //改变数据库消息状态
        ChatMessage.changMessageStatus(message, MessageStatus.SUCCESS)
        var chatMessage = ChatMessage.findMessageId(message.stanzaId)
        //回调主线程
        XmppUtils.runMain(Consumer {
            for (listener in sendListeners) {
                listener.onSendMessage(to, message, chatMessage, chat)
            }
        })
    }
}