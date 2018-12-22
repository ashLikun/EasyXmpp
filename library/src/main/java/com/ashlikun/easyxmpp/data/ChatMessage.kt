package com.ashlikun.easyxmpp.data

import com.ashlikun.easyxmpp.XmppManage
import com.ashlikun.easyxmpp.XmppUtils
import com.ashlikun.easyxmpp.status.MessageStatus
import com.ashlikun.orm.LiteOrmUtil
import com.ashlikun.orm.db.annotation.PrimaryKey
import com.ashlikun.orm.db.annotation.Table
import com.ashlikun.orm.db.assit.QueryBuilder
import com.ashlikun.orm.db.assit.WhereBuilder
import com.ashlikun.orm.db.enums.AssignType
import com.ashlikun.orm.db.model.ColumnsValue
import com.ashlikun.orm.db.model.ConflictAlgorithm
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.delay.DelayInformationManager
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.stringprep.XmppStringprepException
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
         * 消息的本地文件路径
         * 用于自己发送图片或者语音或者文件的本地路径
         * 本地路径可能会失效
         */
        var localFile: String?,
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
         * 是否读取
         */
        var isRead: Boolean
) {

    fun date(): Date? = XmppUtils.parseDatetime(dataTime ?: "")

    /**
     * 计算与另一个message的时间间隔ms
     */
    fun compare(tt: ChatMessage): Long {
        return if (date() == null || tt.date() == null) {
            Long.MAX_VALUE
        } else {
            date()!!.time - tt.date()!!.time
        }
    }

    /**
     * 获取xmpp的message对象
     */
    fun getMessage(): Message {
        val message = Message()
        message.stanzaId = messageId
        message.body = content
        message.type = Message.Type.chat
        if (friendUsername != null) {
            try {
                message.to = JidCreate.entityBareFrom(XmppUtils.getJidName(friendUsername!!))
            } catch (e: XmppStringprepException) {
                e.printStackTrace()
            }
        }
        return message
    }

    /**
     * 发送一条消息
     * 必须有[friendUsername]
     *
     * @return 是否加入发送消息的队列成功，具体发送成功请参考[com.ashlikun.easyxmpp.listener.ExMessageListener.messagSendListener]
     *          1:发送成功
     *          -1：检查是否具备发送条件错误，数据都没去保存呢
     *          -2：发送的时候错误，数据已经保存本地了
     */
    fun send(): Int {
        return if (friendUsername != null) {
            send(XmppManage.getChatM().getChat(friendUsername!!))
        } else {
            -1
        }
    }

    /**
     * 发送一条消息给Chat
     * @return 是否加入发送消息的队列成功，具体发送成功请参考[com.ashlikun.easyxmpp.listener.ExMessageListener.messagSendListener]
     *          1:发送成功
     *          -1：检查是否连接错误，数据都没去保存呢
     *          -2：发送过程错误，没有连接服务器
     *          -3: 拦截错误
     */
    fun send(chat: Chat?): Int {
        return if (!XmppManage.isConnected() || !XmppManage.isAuthenticated() || chat == null) -1 else
            try {
                var message = getMessage()
                if (message.to == null) {
                    message.to = chat?.xmppAddressOfChatPartner
                }
                if (friendUsername == null) {
                    friendUsername = message.to.localpartOrNull.toString()
                }
                //先保存数据库,在发送回调的时候再改变状态
                messageStatus = MessageStatus.SENDING
                if (save()) {
                    chat?.send(message)
                    1
                } else {
                    -1
                }
            } catch (e: SmackException.NotConnectedException) {
                e.printStackTrace()
                messageId?.let {
                    changMessageStatus(messageId!!, MessageStatus.ERROR)
                }
                -2
            } catch (e: InterruptedException) {
                e.printStackTrace()
                messageId?.let {
                    changMessageStatus(messageId!!, MessageStatus.ERROR)
                }
                -3
            }
    }

    fun save(): Boolean {
        return try {
            if (content == null || messageId == null) {
                false
            } else {
                LiteOrmUtil.get().save(this) > 0
            }
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
                    "",
                    message.to?.localpartOrNull?.toString(),
                    XmppManage.getCM().getUserName(),
                    XmppUtils.formatDatetime(Date()),
                    true, false
            )
        }

        /**
         * 构建一个我收到的消息
         */
        fun getMyAcceptMessage(message: Message): ChatMessage {
            //如果date不为null就代表是离线消息，时间得用离线消息时间
            var date = DelayInformationManager.getDelayTimestamp(message)
            return ChatMessage(message.stanzaId,
                    MessageStatus.SUCCESS,
                    message.body,
                    "",
                    message.from?.localpartOrNull?.toString(),
                    XmppManage.getCM().getUserName(),
                    XmppUtils.formatDatetime(date ?: Date()),
                    false, false
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

        /**
         * 改变消息状态,成功
         */
        fun changMessageStatus(messageId: String, @MessageStatus.Code status: Int): Boolean {
            return try {
                LiteOrmUtil.get().update(WhereBuilder.create(ChatMessage::class.java).where("messageId = ?", messageId)
                        , ColumnsValue(arrayOf("messageStatus"), arrayOf(status)), ConflictAlgorithm.None) > 0
            } catch (e: Exception) {
                false
            }
        }

        /**
         * 查找一个消息
         */
        fun findMessageId(messageId: String): ChatMessage? {
            return try {
                LiteOrmUtil.get().queryById(messageId, ChatMessage::class.java)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * 查找是否有这条消息
         */
        fun havaMessage(messageId: String): Boolean {
            return try {
                LiteOrmUtil.get().queryById(messageId, ChatMessage::class.java) != null
            } catch (e: Exception) {
                false
            }
        }

        /**
         * 查找是否有这条消息
         */
        fun havaMessage(message: ChatMessage): Boolean {
            return havaMessage(message.messageId ?: "")
        }

        /**
         * 查找是否有这条消息,接收的消息
         */
        fun havaAcceptMessage(message: Message): Boolean {
            return try {
                LiteOrmUtil.get().queryCount(QueryBuilder(ChatMessage::class.java)
                        .where("messageId = ?", message.stanzaId)
                        .whereAnd("friendUsername = ?", message.from?.localpartOrNull?.toString())
                        .whereAnd("isMeSend = ?", false)) > 0
            } catch (e: Exception) {
                false
            }
        }
    }
}