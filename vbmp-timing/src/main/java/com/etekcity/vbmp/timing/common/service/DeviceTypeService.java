package com.etekcity.vbmp.timing.common.service;

import com.etekcity.vbmp.timing.common.bean.DeviceType;

public interface DeviceTypeService {
    /**
     * 根据model查找设备类型信息
     *
     * @param uuid
     * @return
     */
    DeviceType findDeviceTypeByModel(String uuid);

}
