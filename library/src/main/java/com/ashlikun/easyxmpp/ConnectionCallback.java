package com.ashlikun.easyxmpp;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　15:18
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：提供给外部监听,对应连接的状态
 * 基于{@link ConnectionListener}
 */
public interface ConnectionCallback {
    /**
     * 连接成功
     */
    void connected(XMPPConnection connection);

    /**
     * 连接中断
     *
     * @param connection
     */
    void connectionError(Exception connection);

}
