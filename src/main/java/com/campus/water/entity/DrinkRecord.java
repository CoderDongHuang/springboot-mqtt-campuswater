package com.campus.water.entity;

import lombok.Data;

@Data
public class DrinkRecord {
    private Integer id;
    private String cardId;
    private String name;      // 饮水卡持有者姓名
    private Integer dispenserId;  // 饮水机ID（主键）
    private String time;  // 时间
    private Integer drink;  // 饮水量
} 