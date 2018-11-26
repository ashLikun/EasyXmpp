package com.ashlikun.easyxmpp;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/26　15:21
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：登录状态的回调
 */
public interface LoginCallback {
    /**
     * 登录失败
     *
     * @param userName 用户名
     * @param password 密码
     */
    void loginError(String userName, String password,Throwable throwable);

    /**
     * 登录成功
     *
     * @param userName 用户名
     * @param password 密码
     */
    void loginSuccess(String userName, String password);
}
