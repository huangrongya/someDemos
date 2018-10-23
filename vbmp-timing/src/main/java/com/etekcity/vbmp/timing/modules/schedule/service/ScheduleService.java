package com.etekcity.vbmp.timing.modules.schedule.service;

import com.etekcity.vbmp.timing.modules.schedule.bean.ScheduleRequest;
import com.etekcity.vbmp.timing.modules.schedule.bean.ScheduleResponse;

public interface ScheduleService {

    ScheduleResponse addSchedule(ScheduleRequest request) throws Exception;

    ScheduleResponse deleteSchedule(ScheduleRequest request);

    ScheduleResponse updateSchedule(ScheduleRequest request) throws Exception;

    ScheduleResponse updateScheduleState(ScheduleRequest request) throws Exception;

    ScheduleResponse getSchedules(ScheduleRequest request);

    ScheduleResponse stopScheduleByPrimaryKey(Integer id);

    ScheduleResponse updateUuidByUuid(String oldUuid, String newUuid);

    /**
     * 根据UUID删除所有 Schedule
     *
     * @param uuid UUID
     */
    ScheduleResponse deleteScheduleByUUID(String uuid);
}
