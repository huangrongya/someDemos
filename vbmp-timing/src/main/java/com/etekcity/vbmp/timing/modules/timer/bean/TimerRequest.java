package com.etekcity.vbmp.timing.modules.timer.bean;

import com.etekcity.vbmp.timing.common.VBMPRequest;
import lombok.Data;

import java.util.List;

@Data
public class TimerRequest extends VBMPRequest {

    private String counterTime;
    private String action;
    private String uuid;
    private Integer timerId;

    private String status;
    private List<Integer> conflictAwayIds;
    private List<Integer> conflictScheduleIds;
    private List<Integer> conflictTimerIds;

}
