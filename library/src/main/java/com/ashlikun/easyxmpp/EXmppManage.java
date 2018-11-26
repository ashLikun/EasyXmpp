package com.ashlikun.easyxmpp;

import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

/**
 * @author　　: 李坤
 * 创建时间: 2018/11/25 12:05
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */

public class EXmppManage {

    /**
     * 对消息的管理，依赖connection
     */
    ChatManager chatManager;
    /**
     * 配置对象
     */
    EasyXmppConfig config;
    EXmppConnectionManage connectionManage;

    private static volatile EXmppManage instance = null;

    private EXmppManage() {
    }

    public static EXmppManage get() {
        //双重校验DCL单例模式
        if (instance == null) {
            //同步代码块
            synchronized (EXmppManage.class) {
                if (instance == null) {
                    //创建一个新的实例
                    instance = new EXmppManage();
                }
            }
        }
        //返回一个实例
        return instance;
    }

    void init(XMPPTCPConnectionConfiguration configuration, EasyXmppConfig config) {
        this.config = config;
        XMPPTCPConnection connection = new XMPPTCPConnection(configuration);
        //设置答复超时
        connection.setReplyTimeout(config.replyTimeout);
        connectionManage = new EXmppConnectionManage(connection);
        connection.addConnectionListener(connectionManage);
        chatManager = ChatManager.getInstanceFor(connection);
        ReconnectionManager.getInstanceFor(connection);
        connectionManage.connect();
    }

    public String getDomain() {
        return connectionManage.getDomain();
    }

    public void connect() {
        connectionManage.connect();
    }

    /**
     * 获取聊天管理器
     *
     * @return
     */
    public ChatManager getChatManager() {
        return chatManager;
    }

    public EXmppConnectionManage getCm() {
        return connectionManage;
    }

    /**
     * 是否登录
     *
     * @return
     */
    public boolean isAuthenticated() {
        return connectionManage.isAuthenticated();
    }

    /**
     * 是否连接
     *
     * @return
     */
    public boolean isConnected() {
        return connectionManage.isConnected();
    }


    public EasyXmppConfig getConfig() {
        return config;
    }
}
