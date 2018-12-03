
[![Release](https://jitpack.io/v/ashLikun/EasyXmpp.svg)](https://jitpack.io/#ashLikun/EasyXmpp)

# **EasyXmpp**
1:利用smack实现xmpp 的核心封装
## 使用方法

build.gradle文件中添加:
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
并且:

```gradle
dependencies {
    implementation 'com.github.ashLikun:EasyXmpp:{latest version}'
    //smack
    implementation 'org.igniterealtime.smack:smack-android:4.3.0'
    implementation 'org.igniterealtime.smack:smack-tcp:4.3.0'
    implementation 'org.igniterealtime.smack:smack-im:4.3.0'
    implementation 'org.igniterealtime.smack:smack-extensions:4.3.0'
    //数据库
    implementation 'com.github.ashLikun:LiteOrm:1.0.5'
}
```
### 1.用法

```java
    //初始化
    XmppConfig.Builder.create(application)
                .host("xmpp.o6o6o.com")
                .isDebug(true)
                .apply()
    //连接管理
    XmppManage.getCM()
    
    //获取聊天对象
     easyChat = EasyChat("test")
    //发送消息
    easyChat.sendMessage("我是李坤,这是第几条$count")
    //查找DB消息
    easyChat.findMessage()
    //设置当前会话状态
    easyChat.setCurrentState()
      
    //获取聊天管理器
    XmppManage.getChatM()
    //获取消息回执管理器
     XmppManage.getDRM()
```

### 混肴
    #sdk不混淆
   -keep class com.ashlikun.easyxmpp.** { *; }
   -keep class org.jivesoftware.** { *; }

