package com.etekcity.vbmp.timing.common.bean;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Data
public class DeviceType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "SELECT next value for MYCATSEQ_GLOBAL")
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

    public DeviceType(Integer id, String type, String typeName, String typeImg, String model, String modelImg, String modelName, String connectionType, String electricity, String power, String latestFirmVersion, String deviceImg, String pid, String configModel, String smartConfigVideoUrl, String apnConfigVideoUrl, Short awayMaxNumber, Short scheduleMaxNumber, Short timerMaxNumber, Integer timerMaxTime) {
        this.id = id;
        this.type = type;
        this.typeName = typeName;
        this.typeImg = typeImg;
        this.model = model;
        this.modelImg = modelImg;
        this.modelName = modelName;
        this.connectionType = connectionType;
        this.electricity = electricity;
        this.power = power;
        this.latestFirmVersion = latestFirmVersion;
        this.deviceImg = deviceImg;
        this.pid = pid;
        this.configModel = configModel;
        this.smartConfigVideoUrl = smartConfigVideoUrl;
        this.apnConfigVideoUrl = apnConfigVideoUrl;
        this.awayMaxNumber = awayMaxNumber;
        this.scheduleMaxNumber = scheduleMaxNumber;
        this.timerMaxNumber = timerMaxNumber;
        this.timerMaxTime = timerMaxTime;
    }

    public DeviceType() {
        super();
    }

}