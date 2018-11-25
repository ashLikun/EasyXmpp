package com.ashlikun.easyxmpp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * @author　　: 李坤
 * 创建时间: 2018/11/25 12:05
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */

public class EasyXmppManage {
    /**
     * 整个Xmpp  tcp连接
     */
    private static XMPPTCPConnection connection;
    /**
     * 对消息的管理，依赖connection
     */
    private static ChatManager chatManager;

    public static void init() {
        //初始化XMPPTCPConnection相关配置
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
        try {
            builder.setXmppDomain("xmpp.o6o6o.com");
            builder.setResource("android");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        //设置主机地址
        builder.setHost("xmpp.o6o6o.com");
        //设置端口号
        builder.setPort(5222);
        //设置连接超时的最大时间
        builder.setConnectTimeout(10000);
        //设置登录openfire的用户名和密码
        builder.setUsernameAndPassword("likun", "likun");
        //设置安全模式
        builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        //
        builder.setSendPresence(true);
        //设置调试日志
        builder.enableDefaultDebugger();
        connection = new XMPPTCPConnection(builder.build());
        //设置答复超时
        connection.setReplyTimeout(10000);
        connection.addConnectionListener(new XmppConnectionListener());
        chatManager = ChatManager.getInstanceFor(connection);
        connect();
    }

    /**
     * 连接服务器
     */
    public static void connect() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    connection.connect();
                    connection.login();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    public static String getDomain() {
        return connection.getXMPPServiceDomain().getDomain().toString();
    }

    /**
     * 获取一个人员在xmpp的名字
     *
     * @return
     */
    public static String getJidName(String name) {
        return name + "@" + connection.getXMPPServiceDomain().getDomain().toString();
    }

    public static Chat getChat(String name) {
        try {
            return chatManager.chatWith(JidCreate.entityBareFrom(getJidName(name)));
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean sendMessage(String name, String context) {
        try {
            Chat chat = getChat(name);
            /**
             * 发送一条消息给
             */

            chat.send(context);
            return true;
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean sendMessage(String name, Message context) {
        try {
            Chat chat = getChat(name);
            /**
             * 发送一条消息给
             */
            chat.send(context);
            return true;
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addIncomingListener(IncomingChatMessageListener listener) {
        chatManager.addIncomingListener(listener);
    }

    public static void addOutgoingListener(OutgoingChatMessageListener listener) {
        chatManager.addOutgoingListener(listener);
    }
}
