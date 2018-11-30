package com.ashlikun.easyxmpp

import com.ashlikun.easyxmpp.data.ChatMessage
import com.ashlikun.easyxmpp.listener.ExMessageListener
import com.ashlikun.easyxmpp.status.MessageStatus
import com.ashlikun.orm.LiteOrmUtil
import com.ashlikun.orm.db.assit.QueryBuilder
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener
import org.jivesoftware.smack.packet.Message
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.stringprep.XmppStringprepException

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　15:44
 * 邮箱　　：496546144@qq.com
 *
 *
 * 功能介绍：聊天管理器
 * 对应消息的操作都在这
 */
class EXmppChatManage private constructor() {
    companion object {
        private val instance by lazy { EXmppChatManage() }
        /**
         * 聊天管理器
         */
        private val chatManager by lazy { ChatManager.getInstanceFor(EXmppManage.getCM().connection) }

        fun get(): EXmppChatManage = instance
        fun getChatM(): ChatManager = chatManager
    }

    private var messageListener = ExMessageListener()

    /**
     * 获取一个Chat
     */
    fun getChat(name: String): Chat? {
        try {
            return chatManager.chatWith(JidCreate.entityBareFrom(EXmppUtils.getJidName(name)))
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
        return if (!EXmppManage.isConnected()) false else
            try {
                val chat = getChat(name) ?: return false
                if (content.to == null) {
                    content.to = chat.xmppAddressOfChatPartner
                }
                //先保存数据库,在发送回调的时候再改变状态
                var chatMessage = ChatMessage.getMySendMessage(content);
                if (chatMessage.save()) {
                    chat.send(content)
                }
                true
            } catch (e: SmackException.NotConnectedException) {
                e.printStackTrace()
                ChatMessage.changMessageStatus(content, MessageStatus.ERROR)
                false
            } catch (e: InterruptedException) {
                e.printStackTrace()
                ChatMessage.changMessageStatus(content, MessageStatus.ERROR)
                false
            }
    }

    /**
     * 查询当前用户对应的所有消息
     */
    fun findMessage(): List<ChatMessage>? {
        return if (!EXmppManage.isAuthenticated()) null else try {
            LiteOrmUtil.get().query(QueryBuilder(ChatMessage::class.java)
                    .where("meUsername = ?", EXmppManage.getCM().userData.getUser())
                    .orderBy("dataTime"))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 查询当前用户对应的所有消息
     * @param friendUsername 对方名字
     */
    fun findMessage(friendUsername: String): List<ChatMessage>? {
        return if (!EXmppManage.isAuthenticated()) null else try {
            LiteOrmUtil.get().query(QueryBuilder(ChatMessage::class.java)
                    .where("meUsername = ?", EXmppManage.getCM().userData.getUser())
                    .where("friendUsername = ?", friendUsername)
                    .orderBy("dataTime"))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 添加接受消息监听
     * 回调在子线程，XMPP默认的
     *
     * @param listener
     */
    fun addIncomingListenerSubThread(listener: IncomingChatMessageListener) {
        messageListener.addIncomingListenerSubThread(listener)
    }

    fun removeIncomingListenerSubThread(listener: IncomingChatMessageListener) {
        messageListener.removeIncomingListenerSubThread(listener)
    }

    /**
     * 添加接受消息监听
     * 回调在主线程
     *
     * @param listener
     */
    fun addIncomingListener(listener: IncomingChatMessageListener) {
        messageListener.addIncomingListener(listener)
    }

    fun removeIncomingListener(listener: IncomingChatMessageListener) {
        messageListener.removeIncomingListener(listener)
    }

    /**
     * 添加发出消息监听
     * 回调在子线程，XMPP默认的
     *
     * @param listener
     */
    fun addOutgoingListenerSubThread(listener: OutgoingChatMessageListener) {
        messageListener.addOutgoingListenerSubThread(listener)
    }

    fun removeOutgoingListenerSubThread(listener: OutgoingChatMessageListener) {
        messageListener.removeOutgoingListenerSubThread(listener)
    }

    /**
     * 添加发出消息监听
     * 回调在主线程
     *
     * @param listener
     */
    fun addOutgoingListener(listener: OutgoingChatMessageListener) {
        messageListener.addOutgoingListener(listener)
    }

    fun removeOutgoingListener(listener: OutgoingChatMessageListener) {
        messageListener.removeOutgoingListener(listener)
    }

}
