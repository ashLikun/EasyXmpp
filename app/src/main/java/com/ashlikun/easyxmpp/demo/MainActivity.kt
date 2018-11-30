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
import com.ashlikun.easyxmpp.EXmppChatManage
import com.ashlikun.easyxmpp.EXmppManage
import com.ashlikun.easyxmpp.EasyXmppConfig
import com.ashlikun.easyxmpp.LoginCallback
import com.ashlikun.easyxmpp.listener.ConnectionCallback
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener

/**
 * @author zhangshun
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
            EXmppChatManage.get().sendMessage("zhaoyang", "我是李坤2,这是第几条$count")
            count++
            if (count >= 10000) {
                return@Runnable
            }
            // sendMessage();
        }, 50)
    }

    private fun offlineMessage() {
        var size = EXmppManage.getOM().messageCount
        Toast.makeText(this, "离线消息一共$size", Toast.LENGTH_LONG).show()
        if (size > 0) {
            EXmppManage.getOM().messages.forEach {
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
        EasyXmppConfig.Builder.create(application)
                .host("xmpp.o6o6o.com")
                .isDebug(true)
                .apply()
        EXmppManage.getCM().addCallback(object : ConnectionCallback {
            override fun connected(connection: XMPPConnection) {
                EXmppManage.getCM().login("likun", "likun", object : LoginCallback {
                    override fun loginError(userName: String, password: String, throwable: Throwable) {
                        Log.e("loginError", "userName   $password")
                    }

                    override fun loginSuccess(userName: String, password: String) {
                        Log.e("loginSuccess", "userName   $password")
                        EXmppManage.getChatM().findMessage()?.forEach {
                            messageSb.append("数据库--> 用户：${it.friendUsername} -- 时间：${it.dataTime} -- 内容：${it.content}")
                            messageSb.append("\n")
                            textView.text = messageSb
                        }
                    }
                })
            }

            override fun connectionError(connection: Exception) {
            }
        })
        EXmppChatManage.get().addIncomingListener(IncomingChatMessageListener { from, message, chat ->
            textViewTopic.text = "发送者:" + from.localpart.toString()
            messageSb.append(message.body)
            messageSb.append("\n")
            textView.text = messageSb
        })
        EXmppChatManage.get().addOutgoingListener(OutgoingChatMessageListener { to, message, chat ->
        })

    }
}
