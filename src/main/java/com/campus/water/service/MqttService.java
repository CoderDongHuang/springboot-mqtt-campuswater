package com.campus.water.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.campus.water.config.MqttTopicConfig;
import com.campus.water.entity.DrinkRecord;
import com.campus.water.entity.WaterCard;
import com.campus.water.entity.WaterDispenser;
import com.campus.water.mapper.DrinkRecordMapper;
import com.campus.water.mapper.WaterCardMapper;
import com.campus.water.mapper.WaterDispenserMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MQTT服务类
 * 负责处理所有MQTT消息的订阅和发布
 * 主要功能：
 * 1. 订阅饮水机数据、饮水卡数据、饮水记录等主题
 * 2. 处理接收到的MQTT消息并存储到数据库
 * 3. 响应查询请求并发布结果
 */
@Slf4j
@Service
public class MqttService implements MqttCallback {
    // MQTT连接配置(MqttConfig)
    @Autowired
    private MqttConnectOptions mqttConnectOptions;

    @Autowired
    private MqttTopicConfig mqttTopicConfig;

    // 数据访问层接口
    @Autowired
    private WaterDispenserMapper waterDispenserMapper;
    @Autowired
    private WaterCardMapper waterCardMapper;
    @Autowired
    private DrinkRecordMapper drinkRecordMapper;

    // MQTT配置参数
    @Value("${mqtt.client-id}")
    private String clientId;

    // MQTT客户端实例
    private MqttClient mqttClient;

    // 定义卡号和人名的对应关系(方便前端根据人名查询历史数据，提前与硬件和前端确定好卡号与人名对应)
    private final Map<String, String> cardNameMap = new HashMap<String, String>() {{
        put("93 27 68 EC", "李想");
        put("D3 F5 BB 27", "张三");
    }};

    // 名字列表（排除"李想"和"张三"）
    private final String[] names = {
            "王五", "赵六", "钱七", "孙八", "周九", "吴十",
            "郑十一", "王十二", "冯十三", "陈十四", "褚十五", "卫十六", "蒋十七", "沈十八",
            "韩十九", "杨二十", "朱二一", "秦二二", "尤二三", "许二四", "何二五", "吕二六",
            "施二七", "张二八", "孔二九", "曹三十", "严三一", "华三二", "金三三", "魏三四"
    };

    // 用于记录已使用的名字索引
    private int nameIndex = 0;

    /**
     * 获取卡号对应的人名
     * 如果是预定义的卡号，返回对应的人名
     * 如果是其他卡号，随机分配一个名字（排除"李想"和"张三"）
     * @param id 饮水卡ID
     * @return 对应的人名
     */
    private String getCardName(String id) {
        // 如果是预定义的卡号，直接返回对应的人名
        if (cardNameMap.containsKey(id)) {
            return cardNameMap.get(id);
        }

        // 如果是其他卡号，随机分配一个名字
        // 如果名字用完了，从头开始
        if (nameIndex >= names.length) {
            nameIndex = 0;
        }

        // 获取新名字并记录
        String name = names[nameIndex++];
        cardNameMap.put(id, name);
        return name;
    }

    /**
     * 初始化MQTT客户端并订阅主题
     * 在服务启动时自动执行
     * 1. 创建MQTT客户端
     * 2. 设置回调
     * 3. 连接到MQTT服务器
     * 4. 订阅所有需要的主题
     */
    @PostConstruct
    public void init() throws MqttException {
        log.info("正在初始化MQTT客户端...");
        mqttClient = new MqttClient(mqttConnectOptions.getServerURIs()[0], clientId);
        mqttClient.setCallback(this);
        mqttClient.connect(mqttConnectOptions);
        log.info("MQTT客户端连接成功");

        // 订阅所有主题
        mqttClient.subscribe(mqttTopicConfig.getSubDispenser());
        mqttClient.subscribe(mqttTopicConfig.getSubIDCard());
        mqttClient.subscribe(mqttTopicConfig.getPubNameData());
        mqttClient.subscribe(mqttTopicConfig.getPubIdData());
        log.info("已订阅所有主题");
    }

    /**
     * MQTT连接断开时的回调方法
     * 当连接断开时自动尝试重新连接
     * @param cause 断开原因
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT连接断开: {}", cause.getMessage());
        try {
            log.info("正在尝试重新连接...");
            mqttClient.reconnect();
            log.info("重新连接成功");
        } catch (MqttException e) {
            log.error("重新连接失败: {}", e.getMessage());
        }
    }

    /**
     * 接收到MQTT消息时的回调方法
     * 根据不同的主题处理不同类型的消息
     * @param topic 消息主题
     * @param message 消息内容
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        log.info("收到主题 [{}] 的消息: {}", topic, payload);
        
        try {
            JSONObject jsonObject = JSON.parseObject(payload);
            // 根据主题分发到不同的处理方法
            if (topic.equals(mqttTopicConfig.getSubDispenser())) {
                handleDispenserData(jsonObject);
            } else if (topic.equals(mqttTopicConfig.getSubIDCard())) {
                handleDrinkRecordData(jsonObject);
            } else if (topic.equals(mqttTopicConfig.getPubNameData())) {
                handleNameQuery(jsonObject);
            } else if (topic.equals(mqttTopicConfig.getPubIdData())) {
                handleIdQuery(jsonObject);
            } else {
                log.warn("未知的主题: {}", topic);
            }
        } catch (Exception e) {
            log.error("处理消息时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 消息发送完成的回调方法
     * @param token 消息令牌
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.debug("消息发送完成");
    }

    /**
     * 处理饮水机数据
     * 保存饮水机的基本信息到数据库
     * @param jsonObject 包含饮水机信息的JSON对象
     */
    private void handleDispenserData(JSONObject jsonObject) {
        try {
            log.info("正在处理饮水机数据...");
            Integer id = jsonObject.getInteger("id");
            String locate = jsonObject.getString("locate");
            String tds = jsonObject.getString("TDS");
            Integer status = jsonObject.getInteger("status");

            // 检查饮水机是否已存在
            WaterDispenser existingDispenser = waterDispenserMapper.findById(id);
            if (existingDispenser != null) {
                log.info("饮水机 [{}] 已存在，更新数据", id);
                // 更新现有数据
                existingDispenser.setLocate(locate);
                existingDispenser.setTds(tds);
                existingDispenser.setStatus(status);
                waterDispenserMapper.update(existingDispenser);
                log.info("饮水机数据更新成功: {}", existingDispenser);
                return;
            }

            // 如果不存在，创建新的饮水机记录
            WaterDispenser dispenser = new WaterDispenser();
            dispenser.setId(id);
            dispenser.setLocate(locate);
            dispenser.setTds(tds);
            dispenser.setStatus(status);

            waterDispenserMapper.insert(dispenser);
            log.info("饮水机数据保存成功: {}", dispenser);
        } catch (Exception e) {
            log.error("处理饮水机数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理饮水记录数据
     * 1. 验证饮水机是否存在（防止因数据库存在数据而报错）
     * 2. 创建饮水记录并保存到数据库
     * @param jsonObject 包含饮水记录信息的JSON对象
     */
    private void handleDrinkRecordData(JSONObject jsonObject) {
        try {
            log.info("正在处理饮水记录数据...");
            // 从JSON中获取数据
            String dispenserId = jsonObject.getString("id");  // 饮水机ID
            String cardId = jsonObject.getString("card_id");  // 饮水卡ID
            String time = jsonObject.getString("time");       // 时间
            Integer drink = jsonObject.getInteger("drink");   // 饮水量

            // 验证饮水机是否存在
            WaterDispenser dispenser = waterDispenserMapper.findById(Integer.parseInt(dispenserId));
            if (dispenser == null) {
                log.warn("饮水机 [{}] 不存在，跳过记录", dispenserId);
                return;
            }

            // 获取卡号对应的人名
            String name = getCardName(cardId);
            if (name == null) {
                log.warn("未找到卡号 [{}] 对应的人名，跳过记录", cardId);
                return;
            }

            // 创建并保存饮水记录
            DrinkRecord record = new DrinkRecord();
            record.setDispenserId(Integer.parseInt(dispenserId));
            record.setCardId(cardId);
            record.setName(name);
            record.setTime(time);
            record.setDrink(drink);

            drinkRecordMapper.insert(record);
            log.info("饮水记录保存成功: {}", record);
        } catch (Exception e) {
            log.error("处理饮水记录数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理按姓名查询请求
     * @param jsonObject 包含查询参数的JSON对象
     */
    private void handleNameQuery(JSONObject jsonObject) {
        try {
            String name = jsonObject.getString("name");
            log.info("正在处理学生 [{}] 的饮水记录查询请求...", name);

            List<DrinkRecord> records = drinkRecordMapper.findByName(name);
            if (records == null || records.isEmpty()) {
                log.warn("未找到学生 [{}] 的饮水记录", name);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "该饮水卡记录不存在");
                publishMessage(mqttTopicConfig.getSubCar(), JSON.toJSONString(errorResponse));
                return;
            }

            List<Map<String, Object>> response = new ArrayList<>();
            for (DrinkRecord record : records) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", record.getDispenserId());  // 使用饮水机ID
                item.put("time", record.getTime());
                item.put("drink", record.getDrink());
                response.add(item);
            }

            publishMessage(mqttTopicConfig.getSubCar(), JSON.toJSONString(response));
            log.info("学生 [{}] 的饮水记录查询结果: {}条记录", name, records.size());
        } catch (Exception e) {
            log.error("处理按姓名查询请求失败: {}", e.getMessage(), e);
            try {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "查询失败：" + e.getMessage());
                publishMessage(mqttTopicConfig.getSubCar(), JSON.toJSONString(errorResponse));
            } catch (Exception ex) {
                log.error("发送错误响应失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 处理按ID查询请求
     * @param jsonObject 包含查询参数的JSON对象
     */
    private void handleIdQuery(JSONObject jsonObject) {
        try {
            Integer id = jsonObject.getInteger("id");
            log.info("正在处理饮水机 [{}] 的饮水记录查询请求...", id);

            WaterDispenser dispenser = waterDispenserMapper.findById(id);
            if (dispenser == null) {
                log.warn("饮水机 [{}] 不存在", id);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "饮水机不存在");
                publishMessage(mqttTopicConfig.getSubRIDdata(), JSON.toJSONString(errorResponse));
                return;
            }

            List<DrinkRecord> records = drinkRecordMapper.findByDispenserId(id);
            List<Map<String, Object>> response = new ArrayList<>();
            for (DrinkRecord record : records) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", record.getDispenserId());
                item.put("time", record.getTime());
                item.put("drink", record.getDrink());
                response.add(item);
            }

            publishMessage(mqttTopicConfig.getSubRIDdata(), JSON.toJSONString(response));
            log.info("饮水机 [{}] 的饮水记录查询结果: {}条记录", id, records.size());
        } catch (Exception e) {
            log.error("处理按ID查询请求失败: {}", e.getMessage(), e);
            try {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "查询失败：" + e.getMessage());
                publishMessage(mqttTopicConfig.getSubRIDdata(), JSON.toJSONString(errorResponse));
            } catch (Exception ex) {
                log.error("发送错误响应失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 发布MQTT消息
     * 将消息发送到指定的主题
     * @param topic 目标主题
     * @param message 消息内容
     */
    public void publishMessage(String topic, String message) {
        try {
            log.info("正在发布消息到主题 [{}]: {}", topic, message);
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttClient.publish(topic, mqttMessage);
            log.info("消息发布成功");
        } catch (MqttException e) {
            log.error("消息发布失败: {}", e.getMessage());
        }
    }

    /**
     * 检查MQTT连接状态
     * @return 连接是否正常
     */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    /**
     * 重新连接MQTT服务器
     */
    public void reconnect() {
        try {
            if (mqttClient != null && !mqttClient.isConnected()) {
                log.info("正在重新连接MQTT服务器...");
                mqttClient.connect(mqttConnectOptions);
                // 重新订阅主题
                mqttClient.subscribe(mqttTopicConfig.getSubDispenser());
                mqttClient.subscribe(mqttTopicConfig.getSubIDCard());
                mqttClient.subscribe(mqttTopicConfig.getPubNameData());
                mqttClient.subscribe(mqttTopicConfig.getPubIdData());
                log.info("MQTT重新连接成功");
            }
        } catch (MqttException e) {
            log.error("MQTT重新连接失败: {}", e.getMessage(), e);
        }
    }
} 