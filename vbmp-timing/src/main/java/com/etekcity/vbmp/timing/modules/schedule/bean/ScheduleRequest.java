package com.etekcity.vbmp.timing.modules.schedule.bean;

import com.etekcity.vbmp.timing.common.VBMPRequest;
import lombok.Data;

import java.util.List;

@Data
public class ScheduleRequest extends VBMPRequest {

    private String uuid;

    private String startTime;

    private String endTime;

    private String startState;

    private String endState;

    private String repeat;

    private String sunTime; // 0 不使用日出日落时间，1使用日出日落时间

    private String longitude;

    private String latitude;

    private String event;
    private Integer scheduleId;

    private String scheduleState;


    private List<Integer> conflictAwayIds;
    private List<Integer> conflictScheduleIds;
    private List<Integer> conflictTimerIds;
    private List<Integer> conflictLightScheduleIds;

}
