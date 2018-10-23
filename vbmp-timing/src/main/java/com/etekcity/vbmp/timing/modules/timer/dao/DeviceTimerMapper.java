package com.etekcity.vbmp.timing.modules.timer.dao;


import com.etekcity.vbmp.timing.modules.timer.bean.DeviceTimer;
import com.etekcity.vbmp.timing.util.MyMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface DeviceTimerMapper extends MyMapper<DeviceTimer> {
    List<DeviceTimer> selectByPrimaryKeys(@Param("ids") List<Integer> ids);

    int updateStatusByIds(@Param("ids") List<Integer> ids, @Param("status") String status);

    @Select("select * from device_timer where uuid = #{uuid}")
    List<DeviceTimer> selectByUuid(@Param("uuid") String uuid);

    @Update("update device_timer set uuid = #{newUuid} where uuid = #{oldUuid}")
    int updateUuidByUuid(@Param("oldUuid") String oldUuid, @Param("newUuid") String newUuid);

    int updateByPrimaryKey(DeviceTimer timer);
}