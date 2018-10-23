package com.etekcity.vbmp.timing.modules.timer.service;

import com.etekcity.vbmp.timing.modules.timer.bean.TimerRequest;
import com.etekcity.vbmp.timing.modules.timer.bean.TimerResponse;

public interface TimerService {
    TimerResponse addTimer(TimerRequest request);

    TimerResponse deleteTimer(Integer integer);

    TimerResponse updateTimer(TimerRequest request);

    TimerResponse updateTimerStatus(TimerRequest request);

    TimerResponse getTimers(TimerRequest request);

    TimerResponse deleteTimerByUuid(String uuid);

    TimerResponse stopTimerByPrimaryKey(Integer timerId);

    TimerResponse updateUuidByUuid(String oldUuid, String newUuid);
}
