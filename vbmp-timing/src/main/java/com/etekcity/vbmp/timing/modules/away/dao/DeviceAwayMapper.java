package com.etekcity.vbmp.timing.modules.away.dao;


import com.etekcity.vbmp.timing.modules.away.bean.DeviceAway;
import com.etekcity.vbmp.timing.util.MyMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface DeviceAwayMapper extends MyMapper<DeviceAway> {
    @Select("select t.* from device_away t  where uuid = #{uuid}")
    List<DeviceAway> selectByUuid(@Param("uuid") String uuid);

    List<DeviceAway> selectByPrimaryKeys(@Param("ids") List<Integer> ids);

    int updateStatusByIds(@Param("ids") List<Integer> ids, @Param("status") String status);

    @Update("update device_away set uuid = #{newUuid} where uuid = #{oldUuid}")
    int updateUuidByUuid(@Param("oldUuid") String oldUuid, @Param("newUuid") String newUuid);

    @Delete("delete from device_away where uuid = #{uuid}")
    void deleteByUuid(@Param("uuid") String uuid);

    int updateByPrimaryKey(DeviceAway away);
}