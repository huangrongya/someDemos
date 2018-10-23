package com.etekcity.vbmp.timing.modules.schedule.bean;

import com.etekcity.vbmp.timing.common.VBMPResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ScheduleResponse extends VBMPResponse {
    public ScheduleResponse(int code, String msg) {
        super(code, msg);
    }

    public ScheduleResponse() {
    }

    private Integer scheduleId;

    private List<DeviceScheduleView> schedules;

    private List<Integer> conflictAwayIds = new ArrayList<>();
    private List<Integer> conflictScheduleIds = new ArrayList<>();
    private List<Integer> conflictTimerIds = new ArrayList<>();
}
