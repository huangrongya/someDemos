package com.etekcity.vbmp.timing.modules.away.bean;

import com.etekcity.vbmp.timing.common.VBMPRequest;
import lombok.Data;

import java.util.List;

@Data
public class AwayRequest extends VBMPRequest {
    private Integer awayId;
    private String startTime;
    private String endTime;
    private String repeat;
    private String uuid;
    private List<Integer> conflictScheduleIds;
    private List<Integer> conflictTimerIds;
    private String awayStatus;
}
