package com.ashlikun.easyxmpp.data

import android.util.Log
import com.ashlikun.easyxmpp.EXmppManage
import com.ashlikun.easyxmpp.EXmppUtils
import com.ashlikun.easyxmpp.status.MessageStatus
import com.ashlikun.orm.LiteOrmUtil
import com.ashlikun.orm.db.annotation.PrimaryKey
import com.ashlikun.orm.db.annotation.Table
import com.ashlikun.orm.db.assit.WhereBuilder
import com.ashlikun.orm.db.enums.AssignType
import com.ashlikun.orm.db.model.ColumnsValue
import com.ashlikun.orm.db.model.ConflictAlgorithm
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.delay.DelayInformationManager
import org.json.JSONObject
import org.jxmpp.util.XmppDateTime
import java.util.*

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/28　16:44
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：聊天的数据
 * 1：自己发送消息时候先保持本地，然后在发送成功回掉时候改变消息状态
 * 2：注意如果数据库有没有发送成功的消息，那么请重新发送
 *
 */
@Table("ChatMessage")
data class ChatMessage(
        /**
         * xmpp的消息id
         */
        @PrimaryKey(AssignType.BY_MYSELF)
        var messageId: String?,
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
        var isMeSend: Boolean
) {
    fun getContentData(): MessageData? {
        return if (content == null) null else try {
            var json = JSONObject(content)
            MessageData(json.getString("type"), json.getString("content"))
        } catch (e: Exception) {
            null
        }
    }

    fun save(): Boolean {
        return try {
            LiteOrmUtil.get().save(this) > 0
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        /**
         * 构建一个我发出的消息
         */
        fun getMySendMessage(message: Message): ChatMessage {
            return ChatMessage(message.stanzaId,
                    MessageStatus.SENDING,
                    message.body,
                    message.to.localpartOrNull.toString(),
                    EXmppManage.getCM().userData.getUser(),
                    EXmppUtils.formatDatetime(Date()),
                    true
            )
        }

        /**
         * 构建一个我收到的消息
         */
        fun getMyAcceptMessage(message: Message): ChatMessage {
            //如果date不为null就代表是离线消息，时间得用离线消息时间
            var date = DelayInformationManager.getDelayTimestamp(message)
            if (date != null) {
                //通知服务器删除这条离线消息
                try {
                    EXmppManage.getOM().deleteMessages(arrayListOf(XmppDateTime.formatXEP0082Date(date)))
                } catch (e: Exception) {
                    if (EXmppManage.get().config.isDebug) {
                        Log.e("deleteMessages", "删除失败$e")
                    }
                }
            }
            return ChatMessage(message.stanzaId,
                    MessageStatus.SUCCESS,
                    message.body,
                    message.from.localpartOrNull.toString(),
                    EXmppManage.getCM().userData.getUser(),
                    EXmppUtils.formatDatetime(date ?: Date()),
                    false
            )
        }

        /**
         * 改变消息状态,成功
         */
        fun changMessageStatus(message: Message, @MessageStatus.Code status: Int): Boolean {
            return try {
                LiteOrmUtil.get().update(WhereBuilder.create(ChatMessage::class.java).where("messageId = ?", message.stanzaId)
                        , ColumnsValue(arrayOf("messageStatus"), arrayOf(status)), ConflictAlgorithm.None) > 0
            } catch (e: Exception) {
                false
            }
        }
    }
}