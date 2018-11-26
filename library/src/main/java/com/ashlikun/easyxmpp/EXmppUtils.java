package com.ashlikun.easyxmpp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　11:05
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：
 */
class EXmppUtils {
    public static void runMain(Consumer<Integer> next) {
        runMain(1, next);
    }

    public static void runMain(int id, Consumer<Integer> next) {
        Observable.just(id).observeOn(AndroidSchedulers.mainThread())
                .subscribe(next);
    }

    public static void runNew(Consumer<Integer> next) {
        runNew(1, next);
    }

    public static void runNew(int id, Consumer<Integer> next) {
        Observable.just(id).observeOn(Schedulers.newThread())
                .subscribe(next);
    }

    /**
     * 获取一个人员在xmpp的名字
     *
     * @return
     */
    public static String getJidName(String name) {
        return name + "@" + EXmppManage.get().getDomain();
    }

    /**
     * 是否有网络连接
     *
     * @return
     */
    public static boolean isNetworkConnected() {
        boolean isConnected = false;
        //获得ConnectivityManager对象
        ConnectivityManager connMgr = (ConnectivityManager) EXmppManage.get().getConfig().application.getSystemService(Context.CONNECTIVITY_SERVICE);
        //获取所有网络连接的信息
        //检测API是不是小于21，因为到了API21之后getNetworkInfo(int networkType)方法被弃用
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            //获取WIFI连接的信息
            NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            //获取移动数据连接的信息
            NetworkInfo dataNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (dataNetworkInfo.isConnected() || wifiNetworkInfo.isConnected()) {
                //有网
                isConnected = true;
            }
        } else {
            Network[] networks = connMgr.getAllNetworks();
            //通过循环将网络信息逐个取出来
            for (int i = 0; i < networks.length; i++) {
                //获取ConnectivityManager对象对应的NetworkInfo对象
                NetworkInfo networkInfo = connMgr.getNetworkInfo(networks[i]);
                if (networkInfo.isConnected()) {
                    //有网
                    isConnected = true;
                    break;
                }
            }
        }
        return isConnected;
    }
}
