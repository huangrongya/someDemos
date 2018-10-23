package com.etekcity.vbmp.timing.common.service;

import com.etekcity.vbmp.timing.common.bean.DeviceInfo;

public interface DeviceInfoService {
    /**
     * 根据uuid查找设备信息
     *
     * @param uuid
     * @return
     */
    DeviceInfo findDeviceByUuid(String uuid);

}
