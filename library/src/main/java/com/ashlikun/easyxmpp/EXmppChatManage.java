package com.ashlikun.easyxmpp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.reactivex.functions.Consumer;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　15:44
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：聊天管理器
 * 对应消息的操作都在这
 */
public class EXmppChatManage implements IncomingChatMessageListener, OutgoingChatMessageListener {
    private static volatile EXmppChatManage instance = null;
    /**
     * 这2个是切换主线程的回调
     */
    private final Set<IncomingChatMessageListener> incomingListeners = new CopyOnWriteArraySet<>();
    private final Set<OutgoingChatMessageListener> outgoingListeners = new CopyOnWriteArraySet<>();

    private EXmppChatManage() {
        addIncomingListenerSubThread(this);
        addOutgoingListenerSubThread(this);
    }

    public static EXmppChatManage get() {
        //双重校验DCL单例模式
        if (instance == null) {
            //同步代码块
            synchronized (EXmppChatManage.class) {
                if (instance == null) {
                    //创建一个新的实例
                    instance = new EXmppChatManage();
                }
            }
        }
        //返回一个实例
        return instance;
    }

    /**
     * 获取一个Chat
     */
    public Chat getChat(String name) {
        try {
            return EXmppManage.get().getChatManager().chatWith(JidCreate.entityBareFrom(EXmppUtils.getJidName(name)));
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean sendMessage(String name, String context) {
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

    public boolean sendMessage(String name, Message context) {
        try {
            Chat chat = getChat(name);
            /**
             * 发送一条消息给Chat
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

    /**
     * 添加接受消息监听
     * 回调在子线程，XMPP默认的
     *
     * @param listener
     */
    public void addIncomingListenerSubThread(IncomingChatMessageListener listener) {
        EXmppManage.get().getChatManager().removeIncomingListener(listener);
        EXmppManage.get().getChatManager().addIncomingListener(listener);
    }

    public void removeIncomingListenerSubThread(IncomingChatMessageListener listener) {
        EXmppManage.get().getChatManager().removeIncomingListener(listener);
    }

    /**
     * 添加接受消息监听
     * 回调在主线程
     *
     * @param listener
     */
    public void addIncomingListener(IncomingChatMessageListener listener) {
        if (!incomingListeners.contains(listener)) {
            incomingListeners.add(listener);
        }
    }

    public void removeIncomingListener(IncomingChatMessageListener listener) {
        if (incomingListeners.contains(listener)) {
            incomingListeners.remove(listener);
        }
    }

    /**
     * 添加发出消息监听
     * 回调在子线程，XMPP默认的
     *
     * @param listener
     */
    public void addOutgoingListenerSubThread(OutgoingChatMessageListener listener) {
        EXmppManage.get().getChatManager().removeOutgoingListener(listener);
        EXmppManage.get().getChatManager().addOutgoingListener(listener);
    }

    public void removeOutgoingListenerSubThread(OutgoingChatMessageListener listener) {
        EXmppManage.get().getChatManager().removeOutgoingListener(listener);
    }

    /**
     * 添加发出消息监听
     * 回调在主线程
     *
     * @param listener
     */
    public void addOutgoingListener(OutgoingChatMessageListener listener) {
        if (!outgoingListeners.contains(listener)) {
            outgoingListeners.add(listener);
        }
    }

    public void removeOutgoingListener(OutgoingChatMessageListener listener) {
        if (outgoingListeners.contains(listener)) {
            outgoingListeners.remove(listener);
        }
    }

    @Override
    public void newIncomingMessage(final EntityBareJid from, final Message message, final Chat chat) {
        //回调主线程
        EXmppUtils.runMain(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                for (IncomingChatMessageListener listener : incomingListeners) {
                    listener.newIncomingMessage(from, message, chat);
                }
            }
        });
    }

    @Override
    public void newOutgoingMessage(final EntityBareJid to, final Message message, final Chat chat) {
        EXmppUtils.runMain(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                for (OutgoingChatMessageListener listener : outgoingListeners) {
                    listener.newOutgoingMessage(to, message, chat);
                }
            }
        });
    }
}
