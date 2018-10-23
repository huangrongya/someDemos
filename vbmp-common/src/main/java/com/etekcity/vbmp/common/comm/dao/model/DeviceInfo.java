package com.etekcity.vbmp.common.comm.dao.model;

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

}