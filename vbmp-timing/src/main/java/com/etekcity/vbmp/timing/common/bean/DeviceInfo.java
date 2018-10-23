package com.etekcity.vbmp.timing.common.bean;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Data
public class DeviceInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "SELECT next value for MYCATSEQ_GLOBAL")
    private Integer id;

    private String accountId;

    private String deviceType;

    private String deviceName;

    private String deviceImg;

    private String deviceCid;

    private String uuid;

    private String type;

    private String deviceConnectionType;

    private String allowNotify;

    private String currentFirmVersion;

    private String ownerShip;

    private String deviceHistoryName;

    private String sharedPeopleId;

    private Date deviceOpenTime;

    private Date deviceCloseTime;

    private String timeZone;

    public DeviceInfo(Integer id, String accountId, String deviceType, String deviceName, String deviceImg, String deviceCid, String uuid, String type, String deviceConnectionType, String allowNotify, String currentFirmVersion, String ownerShip, String deviceHistoryName, String sharedPeopleId, Date deviceOpenTime, Date deviceCloseTime, String timeZone) {
        this.id = id;
        this.accountId = accountId;
        this.deviceType = deviceType;
        this.deviceName = deviceName;
        this.deviceImg = deviceImg;
        this.deviceCid = deviceCid;
        this.uuid = uuid;
        this.type = type;
        this.deviceConnectionType = deviceConnectionType;
        this.allowNotify = allowNotify;
        this.currentFirmVersion = currentFirmVersion;
        this.ownerShip = ownerShip;
        this.deviceHistoryName = deviceHistoryName;
        this.sharedPeopleId = sharedPeopleId;
        this.deviceOpenTime = deviceOpenTime;
        this.deviceCloseTime = deviceCloseTime;
        this.timeZone = timeZone;
    }

    public DeviceInfo() {
    }
}