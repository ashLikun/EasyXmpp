package com.ashlikun.easyxmpp

import com.ashlikun.easyxmpp.data.ChatMessage
import com.ashlikun.easyxmpp.listener.ExMessageListener
import com.ashlikun.easyxmpp.listener.ReceiveMessageListener
import com.ashlikun.easyxmpp.listener.SendMessageListener
import com.ashlikun.orm.LiteOrmUtil
import com.ashlikun.orm.db.assit.QueryBuilder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
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
class EXmppChatManage internal constructor(connection: XMPPTCPConnection) {

    /**
     * 聊天管理器
     */
    private val chatManager: ChatManager = ChatManager.getInstanceFor(connection)

    private var messageListener = ExMessageListener(connection, chatManager)


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
     * @param friendUsername  对方名字 如果是null就是全部的
     */
    fun findMessage(friendUsername: String? = null, callback: (List<ChatMessage>?) -> Unit) {
        Observable.create<List<ChatMessage>?> {
            it.onNext(if (!XmppManage.getCM().userData.isValid()) null else try {
                var query = QueryBuilder(ChatMessage::class.java)
                        .where("meUsername = ?", XmppManage.getCM().getUserName())
                if (friendUsername?.isNotEmpty() == true) {
                    query.whereAnd("friendUsername = ?", friendUsername)
                }
                query.orderBy("dataTime")
                LiteOrmUtil.get().query(query)
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
     * @param friendUsername 对方名字 如果是null就是全部的
     * @param start 开始的行数
     * @param pageSize 查询多少条数据
     */
    fun findMessage(friendUsername: String? = null, start: Int, pageSize: Int, callback: (List<ChatMessage>?) -> Unit) {
        Observable.create<List<ChatMessage>?> {
            it.onNext(if (!XmppManage.getCM().userData.isValid()) null else try {
                var query = QueryBuilder(ChatMessage::class.java)
                        .where("meUsername = ?", XmppManage.getCM().getUserName())
                if (friendUsername?.isNotEmpty() == true) {
                    query.whereAnd("friendUsername = ?", friendUsername)
                }
                query.orderBy("dataTime").limit(start, pageSize)
                LiteOrmUtil.get().query(query)
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
     * 查询当前用户对应的所有 未读消息
     * @param friendUsername  对方名字 如果是null就是全部的
     */
    fun findUnreadMessage(friendUsername: String? = null, callback: (List<ChatMessage>?) -> Unit) {
        Observable.create<List<ChatMessage>?> {
            it.onNext(if (!XmppManage.getCM().userData.isValid()) null else try {
                var query = QueryBuilder(ChatMessage::class.java)
                        .where("meUsername = ?", XmppManage.getCM().getUserName())
                        .whereAnd("isRead = ?", false)
                if (friendUsername?.isNotEmpty() == true) {
                    query.whereAnd("friendUsername = ?", friendUsername)
                }
                query.orderBy("dataTime")
                LiteOrmUtil.get().query(query)
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
     * 查询当前用户对应的所有 未读消息 数量
     * @param friendUsername 对方名字 如果是null就是全部的
     */
    fun findUnreadCount(friendUsername: String? = null, callback: (Long) -> Unit) {
        Observable.create<Long> {
            it.onNext(if (!XmppManage.getCM().userData.isValid()) null else try {
                var query = QueryBuilder(ChatMessage::class.java)
                        .where("meUsername = ?", XmppManage.getCM().getUserName())
                        .whereAnd("isRead = ?", false)
                if (friendUsername?.isNotEmpty() == true) {
                    query.whereAnd("friendUsername = ?", friendUsername)
                }
                query.orderBy("dataTime")
                LiteOrmUtil.get().queryCount(query)
            } catch (e: Exception) {
                null
            })
        }.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback, {
                    callback(0)
                })
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
