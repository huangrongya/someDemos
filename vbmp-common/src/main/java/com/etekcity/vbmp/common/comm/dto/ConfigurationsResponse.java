package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.config.VBMPResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigurationsResponse extends VBMPResponse {
    private String deviceName;
    private String deviceImg;
    private String allowNotify;
    private String currentFirmVersion;
    private String latestFirmVersion;
    private Boolean ownerShip;
    // wifi-switch-1.3省电模式,功率保护模式
    private String energySavingStatus;
    private Double maxCost;
    private Double costPerKWH;
    private String currency;
    // wifi-switch-1.3功率保护模式
    private String powerProtectionStatus;
    // 单位:w
    private Float threshold;
    private Float maxPower;
    // 15A插座
    private String nightLightAutomode;
    private String nightLightName;
    private String defaultDeviceImg;

}
