package com.ashlikun.easyxmpp

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　11:05
 * 邮箱　　：496546144@qq.com
 *
 *
 * 功能介绍：
 */
object XmppUtils {
    /**
     * 是否有网络连接
     */
    fun isNetworkConnected(): Boolean {
        var isConnected = false
        val connMgr = XmppManage.get().config.application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // 检测API是不是小于21，因为到了API21之后getNetworkInfo(int networkType)方法被弃用
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {

            val wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            val dataNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
            if (dataNetworkInfo.isConnected || wifiNetworkInfo.isConnected) {
                isConnected = true
            }
        } else {
            val networks = connMgr.allNetworks
            //通过循环将网络信息逐个取出来
            for (i in networks.indices) {
                val networkInfo = connMgr.getNetworkInfo(networks[i])
                if (networkInfo?.isConnected == true) {
                    isConnected = true
                    break
                }
            }
        }
        return isConnected
    }

    fun runMain(next: () -> Unit) {
        runMain(1, next)
    }

    fun runMain(id: Int, next: () -> Unit) {
        Observable.just(id).observeOn(AndroidSchedulers.mainThread())
                .subscribe { next() }
    }

    fun runNew(next: () -> Unit): Disposable {
        return runNew(1, next)
    }

    fun runNew(id: Int, next: () -> Unit): Disposable {
        return Observable.just(id).observeOn(Schedulers.computation())
                .subscribe { next() }
    }

    /**
     * 获取一个人员在xmpp的名字
     */
    fun getJidName(name: String): String {
        return "$name@${XmppManage.get().getDomain()}"
    }


    fun loge(msg: String) {
        if (XmppManage.get().config.isDebug) {
            Log.d("EasyXmpp", msg)
        }
    }

    fun getFrom(): Jid? {
        return XmppManage.getCM().connection?.user?.asEntityBareJid()
                ?: JidCreate.from(XmppManage.getCM().connection.configuration?.username?.toString() + "@" + XmppManage.getCM().connection.configuration.xmppServiceDomain.toString())
    }


}
