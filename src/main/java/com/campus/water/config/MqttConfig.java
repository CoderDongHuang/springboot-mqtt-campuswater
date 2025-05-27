package com.campus.water.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT配置类，用于配置MQTT客户端的连接选项
 * @author itdonghuang
 * @date 2025-05-20
 */
@Configuration
public class MqttConfig {
    // 从配置文件中获取MQTT代理地址
    @Value("${mqtt.broker}")
    private String broker;

    // 从配置文件中获取MQTT客户端ID
    @Value("${mqtt.client-id}")
    private String clientId;

    // 从配置文件中获取MQTT用户名
    @Value("${mqtt.username}")
    private String username;

    // 从配置文件中获取MQTT密码
    @Value("${mqtt.password}")
    private String password;

    /**
     * 创建并配置MQTT连接选项
     * @return 配置好的MQTT连接选项对象
     */
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        // 设置MQTT代理地址
        options.setServerURIs(new String[]{broker});
        // 设置用户名
        options.setUserName(username);
        // 设置密码
        options.setPassword(password.toCharArray());
        // 设置连接超时时间
        options.setConnectionTimeout(60);
        // 设置心跳间隔
        options.setKeepAliveInterval(60);
        // 启用自动重连
        options.setAutomaticReconnect(true);
        // 启用清洁会话
        options.setCleanSession(true);
        // 设置最大重连延迟
        options.setMaxReconnectDelay(5000);
        // 设置MQTT版本
        options.setMqttVersion(4);
        return options;
    }
}
