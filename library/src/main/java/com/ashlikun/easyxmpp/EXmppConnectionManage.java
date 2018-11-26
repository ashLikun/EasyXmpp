package com.ashlikun.easyxmpp;

import android.text.TextUtils;
import android.util.Log;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　16:37
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：连接管理器,对应连接的一些操作
 */
public class EXmppConnectionManage extends AbstractConnectionListener {
    public static final String TAG = "EXmppConnectionManage";
    /**
     * 整个Xmpp  tcp连接
     */
    XMPPTCPConnection connection;

    public List<ConnectionCallback> callbackList = new ArrayList<>();
    /**
     * 失败累计计数
     */
    private float count = 1;
    /**
     * 用户名与密码
     */
    String userName;
    String password;

    EXmppConnectionManage(XMPPTCPConnection connection) {
        this.connection = connection;
    }

    /**
     * 连接服务器
     * 未处理异常
     */
    void connectSub() throws Exception {
        if (!EXmppManage.get().isConnected()) {
            connection.connect();
        }
    }

    /**
     * 连接服务器
     * 处理异常
     */
    public void connect() {
        Observable.just(1).observeOn(Schedulers.newThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        if (!connection.isConnected()) {
                            connection.connect();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        reconnection();
                    }
                });
    }

    private int getTime() {
        return (int) (EXmppManage.get().config.reconnectionTime * count);
    }

    private void reconnection() {
        if (!EXmppManage.get().config.isReconnection()) {
            return;
        }
        count = 1;
        start();
    }

    private void start() {
        Observable.timer(getTime(), TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (!EXmppManage.get().isConnected()) {
                            connectSub();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        //异常了继续重新连接
                        count++;
                        start();
                    }
                });
    }

    /**
     * 登录
     */
    public void login(final LoginCallback callback) {
        login(userName, password, callback);
    }

    /**
     * 登录,如果失败在重新连接的时候会登录
     */
    public void login(final String userName, final String password, final LoginCallback callback) {
        this.userName = userName;
        this.password = password;
        Observable.just(1)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<Integer, Boolean>() {
                    @Override
                    public Boolean apply(Integer integer) throws Exception {
                        connection.login(userName, password);
                        return connection.isAuthenticated();
                    }
                })
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean isAuthenticated) throws Exception {
                        if (callback != null) {
                            if (isAuthenticated) {
                                callback.loginSuccess(userName, password);
                            } else {
                                callback.loginError(userName, password, new Exception("isAuthenticated == false"));
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        //登录失败，回调
                        if (callback != null) {
                            callback.loginError(userName, password, throwable);
                        }
                    }
                });
    }

    /**
     * 添加接受消息监听
     *
     * @param callback
     */
    public void addCallback(ConnectionCallback callback) {
        if (!callbackList.contains(callback)) {
            callbackList.add(callback);
        }
    }

    public void removeCallback(ConnectionCallback callback) {
        if (callbackList.contains(callback)) {
            callbackList.remove(callback);
        }
    }

    /**
     * 获取用户名
     *
     * @return
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 获取密码
     *
     * @return
     */
    public String getPassword() {
        return password;
    }

    public void setUserName(final String userName, final String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public void connected(XMPPConnection connection) {
        super.connected(connection);
        if (EXmppManage.get().getConfig().isDebug) {
            Log.e(TAG, "连接成功");
        }
        //如果有登录信息会再次登录
        if (!TextUtils.isEmpty(getUserName()) && !TextUtils.isEmpty(getPassword())) {
            if (EXmppManage.get().isConnected() && !EXmppManage.get().isAuthenticated()) {
                login(null);
            }
        }
        for (ConnectionCallback callback : callbackList) {
            callback.connected(connection);
        }
    }

    /**
     * 登录成功
     *
     * @param connection
     * @param resumed
     */
    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        super.authenticated(connection, resumed);
        if (EXmppManage.get().getConfig().isDebug) {
            Log.e(TAG, "authenticated" + resumed);
        }
    }

    /**
     * 连接关闭
     */
    @Override
    public void connectionClosed() {
        super.connectionClosed();
        if (EXmppManage.get().getConfig().isDebug) {
            Log.e(TAG, "connectionClosed");
        }
        for (ConnectionCallback callback : callbackList) {
            callback.connectionError(new Exception("connection is closed"));
        }
    }

    /**
     * 是否登录
     *
     * @return
     */
    public boolean isAuthenticated() {
        return connection.isAuthenticated();
    }

    /**
     * 是否连接
     *
     * @return
     */
    public boolean isConnected() {
        return connection.isConnected();
    }

    public String getDomain() {
        return connection.getXMPPServiceDomain().getDomain().toString();
    }

    /**
     * 连接错误
     *
     * @param e
     */
    @Override
    public void connectionClosedOnError(Exception e) {
        super.connectionClosedOnError(e);
        if (EXmppManage.get().getConfig().isDebug) {
            Log.e(TAG, "connectionClosedOnError" + e.toString());
        }
        for (ConnectionCallback callback : callbackList) {
            callback.connectionError(e);
        }
    }
}
