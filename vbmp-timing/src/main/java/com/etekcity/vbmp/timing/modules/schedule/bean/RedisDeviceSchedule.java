package com.etekcity.vbmp.timing.modules.schedule.bean;

import lombok.Data;

@Data
public class RedisDeviceSchedule extends DeviceSchedule {
    private long minutes;
    private String type;
}