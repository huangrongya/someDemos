package com.etekcity.vbmp.timing.modules.timer.bean;

import com.etekcity.vbmp.timing.common.VBMPResponse;
import com.etekcity.vbmp.timing.modules.schedule.bean.DeviceSchedule;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TimerResponse extends VBMPResponse {
    public TimerResponse(int code, String msg) {
        super(code, msg);
    }

    public TimerResponse() {
    }

    private Integer timerId;
    private List<DeviceTimer> timers;
    private List<Integer> conflictAwayIds = new ArrayList<>();
    private List<Integer> conflictScheduleIds = new ArrayList<>();
    private List<Integer> conflictTimerIds = new ArrayList<>();
}
