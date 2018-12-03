package com.ashlikun.easyxmpp.data

import com.ashlikun.easyxmpp.XmppManage
import com.ashlikun.easyxmpp.status.MessageStatus
import com.ashlikun.orm.LiteOrmUtil
import com.ashlikun.orm.db.assit.QueryBuilder
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.chatstates.ChatState
import org.jivesoftware.smackx.chatstates.ChatStateManager

/**
 * 作者　　: 李坤
 * 创建时间: 2018/12/1　10:04
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：与用户聊天的对象封装
 * @param friendUsername 聊天好友的用户名,群聊时为群聊的jid
 */
class EasyChat constructor(var friendUsername: String) {
    /**
     * 当前用户信息
     */
    var user: User
    /**
     * xmpp的聊天对象
     */
    var chat: Chat? = null

    init {
        chat = XmppManage.getChatM().getChat(friendUsername)
        user = XmppManage.getCM().userData
    }

    fun getUserName(): String = user.userName

    /**
     * 发送一条消息给Chat
     */
    fun sendMessage(content: String): Boolean {
        val stanza = Message()
        stanza.body = content
        stanza.type = Message.Type.chat
        return sendMessage(stanza)
    }

    /**
     * 发送一条消息给Chat
     */
    fun sendMessage(content: Message): Boolean {
        return if (!XmppManage.isConnected() || chat == null) false else
            try {
                if (content.to == null) {
                    content.to = chat?.xmppAddressOfChatPartner
                }
                //先保存数据库,在发送回调的时候再改变状态
                var chatMessage = ChatMessage.getMySendMessage(content);
                if (chatMessage.save()) {
                    chat?.send(content)
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
        return if (user.userName.isEmpty()) null else try {
            LiteOrmUtil.get().query(QueryBuilder(ChatMessage::class.java)
                    .where("meUsername = ?", user.getUser())
                    .where("friendUsername = ?", friendUsername)
                    .orderBy("dataTime"))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 设置当前会话状态
     * active（参加会话）, composing（正在输入）, gone（离开）, inactive（没有参加会话）, paused（暂停输入）。
     */
    fun setCurrentState(newState: ChatState) {
        if (XmppManage.isConnected()) {
            ChatStateManager.getInstance(XmppManage.getCM().connection).setCurrentState(newState, chat)
        }
    }
}