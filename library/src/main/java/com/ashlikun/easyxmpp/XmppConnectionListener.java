package com.ashlikun.easyxmpp;

import android.util.Log;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.XMPPConnection;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/25　14:12
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */
public class XmppConnectionListener extends AbstractConnectionListener {
    public static final String TAG = "XmppConnection";

    @Override
    public void connected(XMPPConnection connection) {
        super.connected(connection);
        Log.e(TAG, "连接成功");
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        super.authenticated(connection, resumed);
        Log.e(TAG, "authenticated" + resumed);
    }

    @Override
    public void connectionClosed() {
        super.connectionClosed();
        Log.e(TAG, "connectionClosed");
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        super.connectionClosedOnError(e);
        Log.e(TAG, "connectionClosedOnError" + e.toString());
    }
}
