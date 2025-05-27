package com.campus.water.mapper;

import com.campus.water.entity.WaterDispenser;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WaterDispenserMapper {
    @Insert("INSERT INTO water_dispenser (id, locate, tds, status) VALUES (#{id}, #{locate}, #{tds}, #{status})")
    void insert(WaterDispenser dispenser);

    @Select("SELECT * FROM water_dispenser WHERE id = #{id}")
    WaterDispenser findById(@Param("id") Integer id);

    @Update("UPDATE water_dispenser SET locate = #{locate}, tds = #{tds}, status = #{status} WHERE id = #{id}")
    void update(WaterDispenser dispenser);
} 