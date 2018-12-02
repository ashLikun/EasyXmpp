package com.ashlikun.easyxmpp.listener

import com.ashlikun.easyxmpp.data.ChatMessage
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.packet.Message
import org.jxmpp.jid.EntityBareJid

/**
 * 作者　　: 李坤
 * 创建时间: 2018/12/1　11:42
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：发送出去的消息监听
 */
interface SendMessageListener {
    fun onSendMessage(to: EntityBareJid, message: Message, dbMessage: ChatMessage?, chat: Chat)
}