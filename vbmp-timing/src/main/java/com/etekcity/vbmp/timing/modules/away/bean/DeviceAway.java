package com.etekcity.vbmp.timing.modules.away.bean;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Data
public class DeviceAway {
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

    private String awayRepeat;

    private String timeZone;

    private String status;

    private Integer switchNo;

    private String accountId;

    private Date turnonTime;

    private Date createTime;


}