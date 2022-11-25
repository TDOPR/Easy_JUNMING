package com.haoliang.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Description 全局静态变量配置
 * @Author Dominick Li
 * @CreateTime 2022/10/20 17:08
 **/
@Component
public class GlobalConfig {


    private static long tokenExpire;

    private static String tokenSecret;

    private static String virtualPathURL;

    private static String tmpSavePath;

    private static Integer webSocketPort;

    private static String webSocketAddress;

    public static Integer getWebSocketPort() {
        return webSocketPort;
    }

    @Value("${webSocket.port}")
    public  void setWebSocketPort(Integer webSocketPort) {
        GlobalConfig.webSocketPort = webSocketPort;
    }

    public static String getWebSocketAddress() {
        return webSocketAddress;
    }

    @Value("${webSocket.address}")
    public  void setWebSocketAddress(String webSocketAddress) {
        GlobalConfig.webSocketAddress = webSocketAddress;
    }

    public static String getVirtualPathURL() {
        return virtualPathURL;
    }

    @Value("${app.virtualPathURL}")
    public  void setVirtualPathURL(String virtualPathURL) {
        GlobalConfig.virtualPathURL = virtualPathURL;
    }

    public static long getTokenExpire() {
        return tokenExpire;
    }

    @Value("${jwt.expire}")
    public  void setTokenExpire(long tokenExpire) {
        GlobalConfig.tokenExpire = tokenExpire;
    }

    public static String getTokenSecret() {
        return tokenSecret;
    }

    @Value("${jwt.secret}")
    public  void setTokenSecret(String tokenSecret) {
        GlobalConfig.tokenSecret = tokenSecret;
    }

    public static String getTmpSavePath() {
        return tmpSavePath;
    }

    @Value("${app.tmpSavePath}")
    public  void setTmpSavePath(String tmpSavePath) {
        GlobalConfig.tmpSavePath = tmpSavePath;
    }
}
