package com.campus.water.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQTT主题配置类
 * 集中管理所有MQTT主题
 */
@Data
@Component
@ConfigurationProperties(prefix = "mqtt.topics")
public class MqttTopicConfig {
    /**
     * 订阅饮水机数据主题
     */
    private String subDispenser;

    /**
     * 订阅饮水记录数据主题
     */
    private String subIDCard;

    /**
     * 订阅按姓名查询主题
     */
    private String pubNameData;

    /**
     * 订阅按ID查询主题
     */
    private String pubIdData;

    /**
     * 发布按姓名查询结果主题
     */
    private String subCar;

    /**
     * 发布按ID查询结果主题
     */
    private String subRIDdata;
} 