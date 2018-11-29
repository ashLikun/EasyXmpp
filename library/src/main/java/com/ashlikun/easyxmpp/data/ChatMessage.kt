package com.ashlikun.easyxmpp.data

import com.ashlikun.easyxmpp.status.MessageStatus
import com.ashlikun.orm.LiteOrmUtil
import com.ashlikun.orm.db.annotation.PrimaryKey
import com.ashlikun.orm.db.annotation.Table
import com.ashlikun.orm.db.enums.AssignType
import org.json.JSONObject

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/28　16:44
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：聊天的大数据
 */
@Table("UserData")
data class ChatMessage(
        /**
         * xmpp的消息id
         */
        @PrimaryKey(AssignType.BY_MYSELF)
        var messageId: String,
        /**
         * 消息状态
         */
        @MessageStatus.Code
        var messageStatus: Int,
        /**
         * 消息内容
         * 这是个json
         */
        var content: String?,
        /**
         * 聊天好友的用户名,群聊时为群聊的jid
         */
        var friendUsername: String?,
        /**
         * 自己的用户名
         */
        var meUsername: String?,
        /**
         * 消息发送接收的时间
         */
        var dataTime: String?,
        /**
         * 当前消息是否自己发出的
         */
        var isMeSend: Boolean,
        /**
         * 是否为群聊记录
         */
        var isMulti: Boolean
) {
    fun getContentData(): MessageData? {
        return if (content == null) null else try {
            var json = JSONObject(content)
            MessageData(json.getString("type"), json.getString("content"))
        } catch (e: Exception) {
            null
        }
    }

    fun save() {
//        LiteOrmUtil.get().delete(WhereBuilder.create(ChatMessage::class.java).where("messageId=?",messageId)
//                .where())
        LiteOrmUtil.get().save(this)
    }
}