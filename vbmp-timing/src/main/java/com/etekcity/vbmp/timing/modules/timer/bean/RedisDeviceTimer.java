package com.etekcity.vbmp.timing.modules.timer.bean;

import lombok.Data;

@Data
public class RedisDeviceTimer extends DeviceTimer {
    private long minutes;
    private String type;
}