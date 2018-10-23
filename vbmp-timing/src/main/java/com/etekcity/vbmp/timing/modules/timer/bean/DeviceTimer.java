package com.etekcity.vbmp.timing.modules.timer.bean;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Data
public class DeviceTimer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "SELECT next value for MYCATSEQ_GLOBAL")
    private Integer id;

    private String uuid;

    private String deviceCid;

    private Integer deviceId;

    private Integer seconds;

    private Date executeTime;

    private String action;

    private String status;

    private Integer switchNo;

    private Integer startTimes;

    private String accountId;

    private Date turnonTime;

    private Date createTime;

}