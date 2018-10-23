package com.etekcity.vbmp.device.control.dao.mapper;

import com.etekcity.vbmp.device.control.dao.model.DeviceGetConrtol;
import com.etekcity.vbmp.device.control.dao.model.DeviceType;
import com.etekcity.vbmp.device.control.dto.InsertConrtolRequest;
import com.etekcity.vbmp.device.control.utils.MyMapper;
import org.apache.ibatis.annotations.Param;

public interface DeviceGetControlMapper extends MyMapper<DeviceGetConrtol> {
    /**
     * 获取协议参数
     *
     * @param identify
     * @return
     */
    DeviceGetConrtol getAgreement(@Param("identify") String identify);

    DeviceType getParameters(@Param("configModel") String configModel);

    void insetConrtolMapper(InsertConrtolRequest request);
}
