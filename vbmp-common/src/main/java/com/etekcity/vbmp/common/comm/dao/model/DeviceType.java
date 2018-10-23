package com.etekcity.vbmp.common.comm.dao.model;

import lombok.Data;

@Data
public class DeviceType {

    private Integer id;
    private String type;
    private String typeName;
    private String typeImg;
    private String model;
    private String modelImg;
    private String modelName;
    private String connectionType;
    private String electricity;
    private String power;
    private String latestFirmVersion;
    private String deviceImg;
    private String pid;
    private String configModel;
    private String smartConfigVideoUrl;
    private String apnConfigVideoUrl;
    private Short awayMaxNumber;
    private Short scheduleMaxNumber;
    private Short timerMaxNumber;
    private Integer timerMaxTime;
    private String firmType;
}