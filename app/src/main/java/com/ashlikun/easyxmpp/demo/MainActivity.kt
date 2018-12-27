package com.ashlikun.easyxmpp.demo

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.ashlikun.easyxmpp.SmackInvocationException
import com.ashlikun.easyxmpp.XmppConfig
import com.ashlikun.easyxmpp.XmppManage
import com.ashlikun.easyxmpp.XmppUtils
import com.ashlikun.easyxmpp.data.ChatMessage
import com.ashlikun.easyxmpp.data.EasyChat
import com.ashlikun.easyxmpp.data.User
import com.ashlikun.easyxmpp.listener.ConnectionCallback
import com.ashlikun.easyxmpp.listener.ReceiveMessageListener
import com.ashlikun.easyxmpp.listener.SendMessageListener
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.packet.Message
import org.jxmpp.jid.EntityBareJid

/**
 * @author　　: 李坤
 * 创建时间: 2018/12/3 14:20
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：
 */

class MainActivity : Activity() {
    private val messageSb = StringBuilder()
    /**
     * 回调时使用
     */
    private val MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0
    lateinit var textView: TextView
    lateinit var textViewTopic: TextView

    internal var count = 1
    internal var handler = Handler()
    lateinit var easyChat: EasyChat
    var user = User("likun", "likun")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textView)
        textViewTopic = findViewById(R.id.textViewTopic)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_PHONE_STATE),
                    MY_PERMISSIONS_REQUEST_READ_PHONE_STATE)
        }
        buildEasyXmppService()
        findViewById<View>(R.id.sendButton).setOnClickListener { sendMessage() }
        findViewById<View>(R.id.offlineButton).setOnClickListener { offlineMessage() }

    }

    private fun sendMessage() {
        handler.postDelayed(Runnable {
            easyChat.sendMessage("{\"content\":\"我是李坤$count\",\"type\":\"1\"}")
            count++
            if (count >= 10000) {
                return@Runnable
            }
            // sendMessage();
        }, 50)
    }

    private fun offlineMessage() {
        var size = XmppManage.getOM().messageCount
        Toast.makeText(this, "离线消息一共$size", Toast.LENGTH_LONG).show()
        if (size > 0) {
            XmppManage.getOM().messages.forEach {
                textViewTopic.text = "发送者:" + it.from.localpartOrNull.toString()
                messageSb.append("离线消息-->$it.body")
                messageSb.append("\n")
                textView.text = messageSb
            }
        }
    }


    /**
     * 构建对象
     */
    private fun buildEasyXmppService() {
        XmppConfig.Builder.create(application)
                .host("xmpp.o6o6o.com")
                .isDebug(true)
                .sendPresence(false)
                .apply()
        XmppManage.getCM().connect()
        XmppManage.getCM().addCallback(object : ConnectionCallback {
            override fun authenticated(connection: XMPPConnection, resumed: Boolean) {

            }

            override fun connectionError(isClose: Boolean, connection: SmackInvocationException) {

            }

            override fun connected(connection: XMPPConnection) {
                user.login { user, isSuccess, throwable ->
                    Log.e("loginIsSuccess$isSuccess", "userName   $user")
                    if (isSuccess) {
                        XmppManage.getChatM().findMessage {
                            it?.forEach { mes ->
                                mes.run {
                                    messageSb.append("${if (isMeSend) "我发送的" else "我接收的"}   用户：$friendUsername -- 时间：$dataTime -- 内容：$content")
                                }
                                messageSb.append("\n\n")
                                textView.text = messageSb
                            }
                        }
                    }
                }
            }
        })
        easyChat = EasyChat("likun2")
        easyChat.addReceiveListener(object : ReceiveMessageListener {
            override fun onReceiveMessage(from: EntityBareJid, message: Message, dbMessage: ChatMessage, chat: Chat) {
                dbMessage?.run {
                    messageSb.append("${if (isMeSend) "我发送的" else "我接收的"}   用户：${friendUsername} -- 时间：${dataTime} -- 内容：${content}")
                }
                messageSb.append("\n\n")
                textView.text = messageSb
            }
        })
        easyChat.addSendListener(object : SendMessageListener {
            override fun onSendMessage(to: EntityBareJid, message: Message, dbMessage: ChatMessage, chat: Chat) {
                dbMessage?.run {
                    messageSb.append("${if (isMeSend) "我发送的" else "我接收的"}   用户：${friendUsername} -- 时间：${dataTime} -- 内容：${content}")
                }
                messageSb.append("\n\n")
                textView.text = messageSb
            }
        })
        XmppManage.getDRM().addReceiptReceivedListener { fromJid, toJid, receiptId, receipt ->
            XmppUtils.loge("消息回执 fromJid : ${fromJid.localpartOrNull} ,toJid : ${toJid.localpartOrNull},receiptId : $receiptId,,receipt : $receipt")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        XmppManage.getCM().disconnect()
    }
}
