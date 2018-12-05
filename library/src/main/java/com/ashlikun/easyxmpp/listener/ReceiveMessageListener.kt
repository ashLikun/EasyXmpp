package com.ashlikun.easyxmpp.listener

import com.ashlikun.easyxmpp.data.ChatMessage
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.packet.Message
import org.jxmpp.jid.EntityBareJid

/**
 * 作者　　: 李坤
 * 创建时间: 2018/12/1　11:37
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：收到消息的回调
 */
interface ReceiveMessageListener {

    fun onReceiveMessage(from: EntityBareJid, message: Message, dbMessage: ChatMessage, chat: Chat)
}