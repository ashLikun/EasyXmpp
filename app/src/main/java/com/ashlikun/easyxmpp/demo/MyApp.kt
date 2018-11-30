package com.ashlikun.easyxmpp.demo

import android.app.Application
import com.ashlikun.orm.LiteOrmUtil

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/30　10:36
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：
 */
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LiteOrmUtil.init(this)
        LiteOrmUtil.setIsDebug(true)
    }
}