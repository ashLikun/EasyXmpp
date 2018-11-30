package com.ashlikun.easyxmpp

import android.content.Context
import android.net.ConnectivityManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.*

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　11:05
 * 邮箱　　：496546144@qq.com
 *
 *
 * 功能介绍：
 */
internal object EXmppUtils {
    private val datetimeFormat = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss")

    /**
     * 是否有网络连接
     */
    fun isNetworkConnected(): Boolean {
        var isConnected = false
        val connMgr = EXmppManage.get().config.application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
                if (networkInfo.isConnected) {
                    isConnected = true
                    break
                }
            }
        }
        return isConnected
    }

    fun runMain(next: Consumer<Int>) {
        runMain(1, next)
    }

    fun runMain(id: Int, next: Consumer<Int>) {
        Observable.just(id).observeOn(AndroidSchedulers.mainThread())
                .subscribe(next)
    }

    fun runNew(next: Consumer<Int>) {
        runNew(1, next)
    }

    fun runNew(id: Int, next: Consumer<Int>) {
        Observable.just(id).observeOn(Schedulers.newThread())
                .subscribe(next)
    }

    /**
     * 获取一个人员在xmpp的名字
     */
    fun getJidName(name: String): String {
        return "$name@${EXmppManage.get().getDomain()}"
    }

    /**
     * 格式化日期时间
     * 日期时间格式yyyy-MM-dd HH:mm:ss
     */
    fun formatDatetime(date: Date): String {
        return datetimeFormat.format(date)
    }
}
