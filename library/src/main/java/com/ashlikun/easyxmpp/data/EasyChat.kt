package com.ashlikun.easyxmpp.data

import com.ashlikun.easyxmpp.XmppManage
import com.ashlikun.easyxmpp.listener.ReceiveMessageListener
import com.ashlikun.easyxmpp.listener.SendMessageListener
import com.ashlikun.orm.LiteOrmUtil
import com.ashlikun.orm.db.assit.QueryBuilder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.chatstates.ChatState
import org.jivesoftware.smackx.chatstates.ChatStateManager
import org.jxmpp.jid.EntityBareJid
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 作者　　: 李坤
 * 创建时间: 2018/12/1　10:04
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：与用户聊天的对象封装
 * @param friendUsername 聊天好友的用户名,群聊时为群聊的jid
 */
class EasyChat constructor(var friendUsername: String) : ReceiveMessageListener, SendMessageListener {


    /**
     * xmpp的聊天对象
     */
    var chat: Chat? = null
    /**
     * 这2个是切换主线程的回调
     */
    private val receiveListeners = CopyOnWriteArraySet<ReceiveMessageListener>()
    private val sendListeners = CopyOnWriteArraySet<SendMessageListener>()

    init {
        chat = XmppManage.getChatM().getChat(friendUsername)
        //添加消息监听
        XmppManage.getChatM().addReceiveListener(this)
        XmppManage.getChatM().addSendListener(this)
    }

    override fun onReceiveMessage(from: EntityBareJid, message: Message, dbMessage: ChatMessage, messageChat: Chat) {
        if (chat == null) {
            return
        }
        if (messageChat == chat) {
            receiveListeners.forEach {
                it.onReceiveMessage(from, message, dbMessage, messageChat)
            }
        }
    }

    override fun onSendMessage(to: EntityBareJid, message: Message, dbMessage: ChatMessage, messageChat: Chat) {
        if (messageChat == chat) {
            sendListeners.forEach {
                it.onSendMessage(to, message, dbMessage, messageChat)
            }
        }
    }

    fun getUserName(): String = XmppManage.getCM().getUserName()

    /**
     * 发送一条消息给Chat
     * @return 是否加入发送消息的队列成功，具体发送成功请参考[com.ashlikun.easyxmpp.listener.ExMessageListener.messagSendListener]
     */
    fun sendMessage(content: String): ChatMessage? {
        val stanza = Message()
        stanza.body = content
        stanza.type = Message.Type.chat
        return sendMessage(stanza)
    }

    /**
     * 发送一条消息给Chat
     * @return 是否加入发送消息的队列成功，具体发送成功请参考[com.ashlikun.easyxmpp.listener.ExMessageListener.messagSendListener]
     */
    fun sendMessage(content: Message): ChatMessage? {
        //先保存数据库,在发送回调的时候再改变状态
        var chatManage = ChatMessage.getMySendMessage(content)
        return if (chatManage.send(chat) == -1) null else chatManage
    }

    /**
     * 创建一个消息
     */
    fun createMessage(content: Message): ChatMessage {
        if (content.to == null) {
            content.to = chat?.xmppAddressOfChatPartner
        }
        return ChatMessage.getMySendMessage(content)
    }

    /**
     * 创建一个消息
     */
    fun createMessage(content: String): ChatMessage {
        val stanza = Message()
        stanza.body = content
        stanza.type = Message.Type.chat
        return createMessage(stanza)
    }

    /**
     * 查询当前用户对应的所有消息
     * dataTime->降序排序 新数据在第0个
     */
    fun findMessage(callback: (List<ChatMessage>?) -> Unit) {
        Observable.create<List<ChatMessage>?> {
            it.onNext(if (!XmppManage.getCM().userData.isValid()) null else try {
                LiteOrmUtil.get().query(QueryBuilder(ChatMessage::class.java)
                        .where("meUsername = ?", getUserName())
                        .whereAnd("friendUsername = ?", friendUsername)
                        .appendOrderDescBy("dataTime"))
            } catch (e: Exception) {
                null
            })
        }.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback, {
                    callback(null)
                })
    }

    /**
     * 查询当前用户对应的所有消息,加上分页
     * @param start 开始的行数
     * @param pageSize 查询多少条数据
     * dataTime->降序排序 新数据在第0个
     */
    fun findMessage(start: Int, pageSize: Int, callback: (List<ChatMessage>?) -> Unit) {
        Observable.create<List<ChatMessage>?> {
            it.onNext(if (!XmppManage.getCM().userData.isValid()) null else try {
                LiteOrmUtil.get().query(QueryBuilder(ChatMessage::class.java)
                        .where("meUsername = ?", getUserName())
                        .whereAnd("friendUsername = ?", friendUsername)
                        .appendOrderDescBy("dataTime").limit(start, pageSize))
            } catch (e: Exception) {
                null
            })
        }.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback, {
                    callback(null)
                })
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
     * 销毁的时候一定要调用这个方法，防止内存泄漏
     */
    fun onDestroy() {
        XmppManage.getChatM().removeReceiveListener(this)
        XmppManage.getChatM().removeSendListener(this)
    }
}