package com.etekcity.vbmp.timing.modules.schedule.dao;


import com.etekcity.vbmp.timing.modules.schedule.bean.DeviceSchedule;
import com.etekcity.vbmp.timing.util.MyMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface DeviceScheduleMapper extends MyMapper<DeviceSchedule> {

    List<DeviceSchedule> selectByPrimaryKeys(@Param("ids") List<Integer> ids);

    int updateStatusByIds(@Param("ids") List<Integer> ids, @Param("status") String status);

    @Select("select * from device_schedule where uuid = #{uuid}")
    List<DeviceSchedule> selectByUuid(String uuid);

    @Update("update device_schedule set uuid = #{newUuid} where uuid = #{oldUuid}")
    int updateUuidByUuid(@Param("oldUuid") String oldUuid, @Param("newUuid") String newUuid);

    @Delete("delete from device_schedule where uuid = #{uuid}")
    void deleteByUuid(@Param("uuid") String uuid);

    int updateByPrimaryKey(DeviceSchedule schedule);
}