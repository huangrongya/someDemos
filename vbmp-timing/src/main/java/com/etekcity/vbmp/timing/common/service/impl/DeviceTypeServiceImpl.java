package com.etekcity.vbmp.timing.common.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.timing.common.bean.DeviceType;
import com.etekcity.vbmp.timing.common.dao.DeviceTypeMapper;
import com.etekcity.vbmp.timing.common.redis.RedisService;
import com.etekcity.vbmp.timing.common.service.DeviceTypeService;
import com.etekcity.vbmp.timing.constant.CommonConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class DeviceTypeServiceImpl implements DeviceTypeService {
    @Autowired
    RedisService redisService;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    DeviceTypeMapper deviceTypeMapper;

    @Override
    public DeviceType findDeviceTypeByModel(String model) {
        Assert.hasLength(model, "findDeviceTypeByUuid方法参数model不能为空");
        DeviceType deviceType = null;
        Map<String, String> deviceTypeMap = null;
        if (redisService.exists(CommonConstant.DEVICE_TYPE_REDIS_KEY)) {
            deviceTypeMap = redisService.getMap(CommonConstant.DEVICE_TYPE_REDIS_KEY);
        } else {
            deviceTypeMap = new HashMap<>();
        }
        deviceType = JSONObject.parseObject(deviceTypeMap.get(model), DeviceType.class);
        if (deviceType == null) {
            deviceType = deviceTypeMapper.selectByModel(model);
            redisService.addMap(CommonConstant.DEVICE_TYPE_REDIS_KEY, model, JSON.toJSONString(deviceType));
        }
        return deviceType;
    }
}
