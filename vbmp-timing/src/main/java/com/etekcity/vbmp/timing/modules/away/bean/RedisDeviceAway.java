package com.etekcity.vbmp.timing.modules.away.bean;

import lombok.Data;

import java.util.Date;

@Data
public class RedisDeviceAway extends DeviceAway {
    private long minutes;
    private Date executeTime;
    private String type;
    private String operator;
}