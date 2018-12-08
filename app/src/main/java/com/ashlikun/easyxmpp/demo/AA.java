package com.ashlikun.easyxmpp.demo;

import com.ashlikun.easyxmpp.XmppManage;
import com.ashlikun.easyxmpp.data.ChatMessage;
import com.ashlikun.easyxmpp.listener.SendMessageListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.EntityBareJid;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/12/1　12:00
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */
public class AA {
    public transient int anInt = 1;
    @Override
    public boolean equals(Object obj) {
        XmppManage.Companion.getChatM().addSendListener(new SendMessageListener() {
            @Override
            public void onSendMessage(@NotNull EntityBareJid to, @NotNull Message message, @Nullable ChatMessage dbMessage, @NotNull Chat chat) {

            }
        });
        return super.equals(obj);

    }
}
