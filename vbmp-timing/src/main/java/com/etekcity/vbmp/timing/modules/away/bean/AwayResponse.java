package com.etekcity.vbmp.timing.modules.away.bean;

import com.etekcity.vbmp.timing.common.VBMPResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AwayResponse extends VBMPResponse {
    private Integer awayId;
    private DeviceAway away;
    private List<Integer> conflictScheduleIds = new ArrayList<>();
    private List<Integer> conflictTimerIds = new ArrayList<>();
}
