package com.etekcity.vbmp.common.comm.service.impl;

import com.etekcity.vbmp.common.config.redis.RedisService;
import com.etekcity.vbmp.common.constant.CommonConstant;
import com.etekcity.vbmp.common.comm.dao.mapper.ModuleWifiOutletMapper;
import com.etekcity.vbmp.common.comm.dao.model.ModuleWifiOutlet;
import com.etekcity.vbmp.common.comm.service.ModuleWifiOutletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModuleWifiOutletServiceImpl implements ModuleWifiOutletService {

    @Autowired
    private ModuleWifiOutletMapper wifiOutletMapper;

    @Autowired
    private RedisService redisService;

    public ModuleWifiOutlet getWifiOutlet(String uuid) {
        String redisModuleWifiOutletKey = CommonConstant.REDIS_KEY_WIFIOUTLET_UUID.concat(uuid);
        ModuleWifiOutlet record;
        if (redisService.exists(redisModuleWifiOutletKey)) {
            record = (ModuleWifiOutlet) redisService.get(redisModuleWifiOutletKey);
            return record;
        }
        record = new ModuleWifiOutlet();
        record.setUuid(uuid);
        record = wifiOutletMapper.selectOne(record);
        if (record != null && record.getId() != null) {
            redisService.set(redisModuleWifiOutletKey, record, CommonConstant.SECONDS_OF_ONEDAY);
        }
        return record;
    }
}
