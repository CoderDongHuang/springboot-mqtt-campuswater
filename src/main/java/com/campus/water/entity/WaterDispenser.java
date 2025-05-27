package com.campus.water.entity;

import lombok.Data;

@Data
public class WaterDispenser {
    private Integer id;        // 饮水机ID
    private String locate;     // 位置
    private String tds;        // TDS值
    private Integer status;    // 状态
} 