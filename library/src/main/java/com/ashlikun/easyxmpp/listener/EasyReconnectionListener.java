/**
 * Copyright 2017 Florian Schmaus.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ashlikun.easyxmpp.listener;

/**
 * @author　　: 李坤
 * 创建时间: 2018/12/2 19:52
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：重连的监听
 */
public interface EasyReconnectionListener {

    /**
     * 重连失败
     */
    void reconnectionFailed(Throwable e);

    /**
     * 重连倒计时
     */
    void reconnectionTime(int time);
}
