package com.ashlikun.easyxmpp

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　15:21
 * 邮箱　　：496546144@qq.com
 *
 *
 * 功能介绍：登录状态的回调
 */
interface LoginCallback {
    /**
     * 登录失败
     *
     * @param userName 用户名
     * @param password 密码
     */
    fun loginError(userName: String, password: String, throwable: Throwable)

    /**
     * 登录成功
     *
     * @param userName 用户名
     * @param password 密码
     */
    fun loginSuccess(userName: String, password: String)
}
