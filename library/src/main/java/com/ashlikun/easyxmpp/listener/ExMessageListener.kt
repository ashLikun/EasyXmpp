package com.ashlikun.easyxmpp.listener

import com.ashlikun.easyxmpp.XmppManage
import com.ashlikun.easyxmpp.XmppUtils
import com.ashlikun.easyxmpp.data.ChatMessage
import com.ashlikun.easyxmpp.status.MessageStatus
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.filter.*
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.id.StanzaIdUtil
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.delay.DelayInformationManager
import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension
import org.jxmpp.jid.EntityBareJid
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/30　10:23
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：发送与接受消息的监听
 */
class ExMessageListener constructor(connection: XMPPTCPConnection, var chatManage: ChatManager) : IncomingChatMessageListener {
    private val MESSAGE_FILTER = AndFilter(
            MessageTypeFilter.NORMAL_OR_CHAT,
            OrFilter(MessageWithBodiesFilter.INSTANCE, StanzaExtensionFilter(XHTMLExtension.ELEMENT, XHTMLExtension.NAMESPACE))
    )
    private val OUTGOING_MESSAGE_FILTER = AndFilter(
            MESSAGE_FILTER,
            ToTypeFilter.ENTITY_FULL_OR_BARE_JID
    )
    /**
     * 是否删除过这个用户的离线消息
     */
    private val isOfflineDeleteMessage = HashMap<String, Boolean>()

    fun cleanOfflineDeleteMessage() {
        isOfflineDeleteMessage.clear()
    }

    /**
     * 这2个是切换主线程的回调
     */
    private val receiveListeners = CopyOnWriteArraySet<ReceiveMessageListener>()
    private val sendListeners = CopyOnWriteArraySet<SendMessageListener>()
    /**
     * 自己发送的消息
     * 监听发出的消息，这里是真的成功发出
     * 这里是子线程
     */
    private val messagSendListener = StanzaListener {
        val message = it as Message
        ChatMessage.changMessageStatus(message, MessageStatus.SUCCESS)
        var chatMessage = ChatMessage.findMessageId(message.stanzaId)
        if (chatMessage != null) {
            XmppUtils.runMain {
                val to = message.to.asEntityBareJidOrThrow()
                val chat = chatManage.chatWith(to)
                for (listener in sendListeners) {
                    listener.onSendMessage(to, message, chatMessage, chat)
                }
            }
        }
    }

    init {
        chatManage.removeIncomingListener(this)
        chatManage.addIncomingListener(this)
        //监听发送的消息，这里是xnpp真的发送成功了
        connection.addStanzaSendingListener(messagSendListener, OUTGOING_MESSAGE_FILTER)
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
        //如果本地已经有了就过滤,因为这里的id可能是对方生成的，所以这里加上多个条件判断重复
        if (message.stanzaId.isNullOrBlank()) {
            return
        }
        handleNewMessage(from, message, chat)
    }

    /**
     * 同步数据库操作
     */
    @Synchronized
    private fun handleNewMessage(from: EntityBareJid, message: Message, chat: Chat) {
        if (!ChatMessage.havaAcceptMessage(message)) {
            //如果本地有这个消息id了，那么从新生成
            if () {
                message.stanzaId = StanzaIdUtil.newStanzaId()
            }
            //保存消息到本地
            ChatMessage.getMyAcceptMessage(message).save()
            var chatMessage = ChatMessage.findMessageId(message.stanzaId)
            if (chatMessage != null) {
                if (isOfflineDeleteMessage[XmppManage.getCM().getUserName()] != true && DelayInformationManager.isDelayedStanza(message)) {
                    //通知服务器删除离线消息
                    try {
                        XmppManage.getOM().deleteMessages()
                        isOfflineDeleteMessage[XmppManage.getCM().getUserName()] = true
                    } catch (e: Exception) {
                        if (XmppManage.get().config.isDebug) {
                            XmppUtils.loge("删除失败$e")
                        }
                    }
                }
                //回调主线程
                XmppUtils.runMain {
                    for (listener in receiveListeners) {
                        listener.onReceiveMessage(from, message, chatMessage, chat)
                    }
                }
            }
        }
    }

}