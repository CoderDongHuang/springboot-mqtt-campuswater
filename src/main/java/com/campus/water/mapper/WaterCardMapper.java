package com.campus.water.mapper;

import com.campus.water.entity.WaterCard;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WaterCardMapper {
    @Insert("INSERT INTO water_card (id, name, time, drink) VALUES (#{id}, #{name}, #{time}, #{drink})")
    void insert(WaterCard card);

    @Update("UPDATE water_card SET time = #{time}, drink = #{drink} WHERE id = #{id}")
    void update(WaterCard card);

    @Select("SELECT * FROM water_card WHERE id = #{id}")
    WaterCard findById(@Param("id") String id);

    @Select("SELECT * FROM water_card WHERE name = #{name}")
    WaterCard findByName(@Param("name") String name);

    @Select("SELECT * FROM water_card WHERE name = #{name}")
    List<WaterCard> findAllByName(@Param("name") String name);
} 