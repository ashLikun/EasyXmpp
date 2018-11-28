package com.ashlikun.easyxmpp.data

/**
 * 作者　　: 李坤
 * 创建时间: 2018/11/28　17:12
 * 邮箱　　：496546144@qq.com
 *
 *
 * 功能介绍：具体的聊天content数据
 * 对于xmpp内部的消息 String 封装的json
 */
data class MessageData(
        /**
         * 消息类型
         */
        var type: String,
        /**
         * 消息体
         *  1：如果是文本消息，就是String
         *  2：如果是图片或者语音，就是对应的文件路径
         *  3：如果是其他那么就是json
         */
        var content: String

) {

}
