package com.etekcity.vbmp.timing.common.service.impl;

import com.alibaba.fastjson.JSON;
import com.etekcity.vbmp.timing.common.bean.DeviceInfo;
import com.etekcity.vbmp.timing.common.dao.DeviceInfoMapper;
import com.etekcity.vbmp.timing.common.redis.RedisService;
import com.etekcity.vbmp.timing.common.service.DeviceInfoService;
import com.etekcity.vbmp.timing.constant.CommonConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

@Service
@Transactional
public class DeviceInfoServiceImpl implements DeviceInfoService {
    @Autowired
    RedisService redisService;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    DeviceInfoMapper deviceInfoMapper;

    @Override
    public DeviceInfo findDeviceByUuid(String uuid) {
        Assert.hasLength(uuid, "findDeviceByUuid方法参数uuid不能为空");
        String redisUuidDKey = CommonConstant.REDIS_KEY_DEVICE_UUID_PREFIX.concat(uuid);
        String objectField = CommonConstant.UUID_DEVICE_OBJECT;
        Object value;
        String deviceStr;
        DeviceInfo deviceInfo = null;
        if ((value = redisService.getMapField(redisUuidDKey, objectField)) != null) {
            deviceStr = String.valueOf(value);
            deviceInfo = JSON.parseObject(deviceStr, DeviceInfo.class);
        } else {
            //根据uuid查找设备信息
            deviceInfo = new DeviceInfo();
            deviceInfo.setUuid(uuid);
            List<DeviceInfo> wifiOutlet15DeviceList = deviceInfoMapper.select(deviceInfo);
            if (wifiOutlet15DeviceList != null && !wifiOutlet15DeviceList.isEmpty()) {
                deviceInfo = wifiOutlet15DeviceList.get(0);
                redisService.addMap(redisUuidDKey, objectField, JSON.toJSONString(deviceInfo), CommonConstant.SECONDS_OF_ONEDAY);
            } else {
                deviceInfo = null;
            }
        }
        return deviceInfo;
    }
}
