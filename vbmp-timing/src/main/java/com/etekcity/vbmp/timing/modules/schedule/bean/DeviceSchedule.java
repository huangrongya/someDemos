package com.etekcity.vbmp.timing.modules.schedule.bean;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Data
public class DeviceSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "SELECT next value for MYCATSEQ_GLOBAL")
    private Integer id;

    private String uuid;

    private String deviceCid;

    private Integer deviceId;

    private String startTime;

    private String endTime;

    private Date executeStartTime;

    private Date executeEndTime;

    private String startAction;

    private String endAction;

    private String scheduleRepeat;

    private String timeZone;

    private String status;

    private String event;

    private Integer switchNo;

    private String startSunTime;

    private String endSunTime;

    private String accountId;

    private Date createTime;

    private Date turnonTime;


}