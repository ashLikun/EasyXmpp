package com.ashlikun.easyxmpp;

import android.app.Application;
import android.text.TextUtils;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　11:14
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：对xmpp的初始化配置
 */
public class EasyXmppConfig {
    Application application;
    /**
     * 服务器名称
     */
    String host;
    /**
     * domain,默认就是服务器名称
     */
    String domain;
    /**
     * 端口 默认5222
     */
    int port = 5222;
    /**
     * 设置端名称
     */
    String resource = "android";
    /**
     * 设置连接最大超时时间
     */
    int connectTimeout = 20000;
    /**
     * 安全模式
     */
    ConnectionConfiguration.SecurityMode securityMode = ConnectionConfiguration.SecurityMode.disabled;
    /**
     * 是否开启压缩
     */
    boolean compressionEnabled = true;
    /**
     * 设置登录状态,不想登录传false
     */
    boolean sendPresence = true;
    /**
     * 是否调试
     */
    boolean isDebug = true;
    /**
     * 需要经过同意才可以添加好友 manual
     * 添加直接通过accept_all
     */
    Roster.SubscriptionMode subscriptionMode = Roster.SubscriptionMode.accept_all;
    /**
     * 设置答复超时
     */
    int replyTimeout = 5000;
    /**
     * 重新连接时间间隔
     */
    long reconnectionTime = 5000;
    /**
     * 重连
     */
    boolean isReconnection = true;


    /**
     * 是否重新连接
     *
     * @return
     */
    public boolean isReconnection() {
        return isReconnection;
    }

    public static class Builder {
        EasyXmppConfig config;

        private Builder(Application application) {
            config = new EasyXmppConfig();
            config.application = application;
        }

        public static Builder create(Application application) {
            return new Builder(application);
        }

        /**
         * 服务器名称
         */
        public Builder host(String host) {
            config.host = host;
            return this;
        }

        /**
         * domain,默认就是服务器名称
         */
        public Builder domain(String domain) {
            config.domain = domain;
            return this;
        }

        /**
         * 端口 默认5222
         */
        public Builder port(int port) {
            config.port = port;
            return this;
        }

        /**
         * 设置端名称
         */
        public Builder resource(String resource) {
            config.resource = resource;
            return this;
        }

        /**
         * 设置连接最大超时时间
         */
        public Builder connectTimeout(int connectTimeout) {
            config.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * 安全模式
         */
        public Builder securityMode(ConnectionConfiguration.SecurityMode securityMode) {
            config.securityMode = securityMode;
            return this;
        }

        /**
         * 是否开启压缩
         */
        public Builder compressionEnabled(boolean compressionEnabled) {
            config.compressionEnabled = compressionEnabled;
            return this;
        }

        /**
         * 设置登录状态,不想登录传false
         */
        public Builder sendPresence(boolean sendPresence) {
            config.sendPresence = sendPresence;
            return this;
        }

        /**
         * 是否调试
         */
        public Builder isDebug(boolean isDebug) {
            config.isDebug = isDebug;
            return this;
        }

        /**
         * 需要经过同意才可以添加好友 manual
         * 添加直接通过accept_all
         */
        public Builder subscriptionMode(Roster.SubscriptionMode subscriptionMode) {
            config.subscriptionMode = subscriptionMode;
            return this;
        }

        /**
         * 设置答复超时
         */
        public Builder replyTimeout(int replyTimeout) {
            config.replyTimeout = replyTimeout;
            return this;
        }

        public Builder isReconnection(boolean isReconnection) {
            config.isReconnection = isReconnection;
            return this;
        }

        /**
         * 重新连接时间间隔，这个是初始化时间，内部会重连时候如果还是失败会每次增加
         * 如果关闭重新连接这里设置0
         */
        public Builder reconnectionTime(int reconnectionTime) {
            config.reconnectionTime = reconnectionTime;
            return this;
        }

        public boolean apply() {
            if (TextUtils.isEmpty(config.host)) {
                return false;
            } else {
                //初始化XMPPTCPConnection相关配置
                XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
                try {
                    //设置domain
                    builder.setXmppDomain(config.domain == null ? config.host : config.domain);
                    //设置端名称
                    builder.setResource(config.resource);
                } catch (XmppStringprepException e) {
                    e.printStackTrace();
                }
                //设置主机地址
                builder.setHost(config.host);
                //设置端口号
                builder.setPort(config.port);
                //设置连接超时的最大时间
                builder.setConnectTimeout(config.connectTimeout);
                //设置安全模式
                builder.setSecurityMode(config.securityMode);
                //是否开启压缩
                builder.setCompressionEnabled(config.compressionEnabled);
                //设置登录状态,不想登录传false
                builder.setSendPresence(config.sendPresence);
                //设置调试日志
                if (config.isDebug) {
                    builder.enableDefaultDebugger();
                }
                //需要经过同意才可以添加好友 manual 添加直接通过accept_all
                Roster.setDefaultSubscriptionMode(config.subscriptionMode);
                //重新连接机制
                ReconnectionManager.setEnabledPerDefault(config.isReconnection);
                ReconnectionManager.setDefaultFixedDelay((int) (config.reconnectionTime / 1000));
                ReconnectionManager.setDefaultReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.FIXED_DELAY);

                EXmppManage.get().init(builder.build(), config);
                return true;
            }
        }
    }


}
