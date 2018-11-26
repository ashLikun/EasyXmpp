package com.ashlikun.easyxmpp.mqtt.demo;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.ashlikun.easyxmpp.ConnectionCallback;
import com.ashlikun.easyxmpp.EXmppChatManage;
import com.ashlikun.easyxmpp.EXmppManage;
import com.ashlikun.easyxmpp.EasyXmppConfig;
import com.ashlikun.easyxmpp.LoginCallback;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.EntityBareJid;

/**
 * @author zhangshun
 */
public class MainActivity extends Activity {
    private StringBuilder messageSb = new StringBuilder();
    /**
     * 回调时使用
     */
    private final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;
    TextView textView;
    TextView textViewTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        textViewTopic = findViewById(R.id.textViewTopic);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
        }
        buildEasyMqttService();
        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();

            }
        });

    }

    private void sendMessage() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                EXmppChatManage.get().sendMessage("zhaoyang", "我是李坤2,这是第几条" + count);
                count++;
                if (count >= 10000) {
                    return;
                }
               // sendMessage();
            }
        }, 50);
    }

    int count = 1;
    Handler handler = new Handler();


    /**
     * 构建EasyMqttService对象
     */
    private void buildEasyMqttService() {
        EasyXmppConfig.Builder.create(getApplication())
                .host("xmpp.o6o6o.com")
                .isDebug(true)
                .apply();
        EXmppManage.get().getCm().addCallback(new ConnectionCallback() {
            @Override
            public void connected(XMPPConnection connection) {
                EXmppManage.get().getCm().login("likun", "likun", new LoginCallback() {
                    @Override
                    public void loginError(String userName, String password, Throwable throwable) {

                    }

                    @Override
                    public void loginSuccess(String userName, String password) {

                    }
                });
            }

            @Override
            public void connectionError(Exception connection) {

            }
        });
        EXmppChatManage.get().addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(final EntityBareJid from, final Message message, Chat chat) {
                Log.e("new from", from.toString());
                Log.e("new message", message.toString());
                textViewTopic.setText("发送者:" + from.getLocalpart().toString());
                messageSb.append(message.getBody());
                messageSb.append("\n");
                textView.setText(messageSb);
            }
        });
        EXmppChatManage.get().addOutgoingListener(new OutgoingChatMessageListener() {
            @Override
            public void newOutgoingMessage(EntityBareJid to, Message message, Chat chat) {
                Log.e("new to", to.toString());
                Log.e("new message", message.toString());
            }
        });

    }
}
