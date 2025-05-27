package com.campus.water.mapper;

import com.campus.water.entity.DrinkRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DrinkRecordMapper {
    @Insert("INSERT INTO drink_record (card_id, name, dispenser_id, time, drink) VALUES (#{cardId}, #{name}, #{dispenserId}, #{time}, #{drink})")
    void insert(DrinkRecord record);

    @Select("SELECT * FROM drink_record WHERE dispenser_id = #{dispenserId}")
    List<DrinkRecord> findByDispenserId(@Param("dispenserId") Integer dispenserId);

    @Select("SELECT * FROM drink_record WHERE dispenser_id = #{dispenserId} AND time = #{time}")
    List<DrinkRecord> findByDispenserIdAndTime(@Param("dispenserId") Integer dispenserId, @Param("time") String time);

    @Select("SELECT * FROM drink_record WHERE card_id = #{cardId}")
    List<DrinkRecord> findByCardId(@Param("cardId") String cardId);

    @Select("SELECT * FROM drink_record WHERE name = #{name}")
    List<DrinkRecord> findByName(@Param("name") String name);
} 