package com.etekcity.vbmp.timing.common.dao;


import com.etekcity.vbmp.timing.common.bean.DeviceType;
import com.etekcity.vbmp.timing.util.MyMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DeviceTypeMapper extends MyMapper<DeviceType> {
    @Select("select id, type, type_name, type_img, model, model_img, model_name, connection_type, " +
            "electricity,power, latest_firm_version, device_img, pid, config_model, smart_config_video_url," +
            "APN_config_video_url, away_max_number, schedule_max_number, timer_max_number, timer_max_time " +
            "from device_type where model = #{model}")
    DeviceType selectByModel(@Param("model") String model);

}