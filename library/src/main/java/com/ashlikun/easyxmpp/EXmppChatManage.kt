package com.ashlikun.easyxmpp

import com.ashlikun.easyxmpp.data.ChatMessage
import com.ashlikun.easyxmpp.listener.ExMessageListener
import com.ashlikun.easyxmpp.listener.ReceiveMessageListener
import com.ashlikun.easyxmpp.listener.SendMessageListener
import com.ashlikun.orm.LiteOrmUtil
import com.ashlikun.orm.db.assit.QueryBuilder
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
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
class EXmppChatManage internal constructor(var connection: XMPPTCPConnection) {
    /**
     * 聊天管理器
     */
    val chatManager: ChatManager

    private var messageListener: ExMessageListener

    init {
        chatManager = ChatManager.getInstanceFor(connection)
        messageListener = ExMessageListener(chatManager)
    }

    fun getChatM(): ChatManager = chatManager
    /**
     * 获取一个Chat
     */
    fun getChat(name: String): Chat? {
        try {
            return chatManager.chatWith(JidCreate.entityBareFrom(XmppUtils.getJidName(name)))
        } catch (e: XmppStringprepException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 查询当前用户对应的所有消息
     */
    fun findMessage(): List<ChatMessage>? {
        return if (!XmppManage.isAuthenticated()) null else try {
            LiteOrmUtil.get().query(QueryBuilder(ChatMessage::class.java)
                    .where("meUsername = ?", XmppManage.getCM().userData.getUser())
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
        return if (!XmppManage.isAuthenticated()) null else try {
            LiteOrmUtil.get().query(QueryBuilder(ChatMessage::class.java)
                    .where("meUsername = ?", XmppManage.getCM().userData.getUser())
                    .where("friendUsername = ?", friendUsername)
                    .orderBy("dataTime"))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 查询当前用户对应的所有消息,加上分页
     * @param friendUsername 对方名字
     * @param start 开始的行数
     * @param pageSize 查询多少条数据
     */
    fun findMessage(friendUsername: String, start: Int, pageSize: Int): List<ChatMessage>? {
        return if (!XmppManage.isAuthenticated()) null else try {
            LiteOrmUtil.get().query(QueryBuilder(ChatMessage::class.java)
                    .where("meUsername = ?", XmppManage.getCM().userData.getUser())
                    .where("friendUsername = ?", friendUsername)
                    .orderBy("dataTime").limit(start, pageSize))
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
     * 添加接受消息监听
     * 回调在主线程
     *
     * @param listener
     */
    fun addReceiveListener(listener: ReceiveMessageListener) {
        messageListener.addReceiveListener(listener)
    }

    fun removeReceiveListener(listener: ReceiveMessageListener) {
        messageListener.removeReceiveListener(listener)
    }

    /**
     * 添加发出消息监听
     * 回调在主线程
     *
     * @param listener
     */
    fun addSendListener(listener: SendMessageListener) {
        messageListener.addSendListener(listener)
    }

    fun removeSendListener(listener: SendMessageListener) {
        messageListener.removeSendListener(listener)
    }

}
