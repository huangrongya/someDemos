package com.etekcity.vbmp.timing.common.service.impl;

import com.etekcity.vbmp.timing.common.VBMPResponse;
import com.etekcity.vbmp.timing.common.service.CommonService;
import com.etekcity.vbmp.timing.constant.ErrorConstant;
import com.etekcity.vbmp.timing.modules.away.service.AwayService;
import com.etekcity.vbmp.timing.modules.schedule.service.ScheduleService;
import com.etekcity.vbmp.timing.modules.timer.service.TimerService;
import com.etekcity.vbmp.timing.util.MyStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommonServiceImpl implements CommonService {

    private Logger logger = LoggerFactory.getLogger(CommonServiceImpl.class);

    @Autowired
    ScheduleService scheduleService;
    @Autowired
    TimerService timerService;
    @Autowired
    AwayService awayService;

    @Override
    public VBMPResponse updateUuidByUuid(String oldUuid, String newUuid) {
        logger.info("updateTimingUuidByUuid oldUuid:{}, newUuid:{}", oldUuid, newUuid);
        VBMPResponse response = new VBMPResponse();
        if (MyStringUtils.isNullData(oldUuid, newUuid)) {
            response.setCode(ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            response.setMsg(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            logger.error("invoke deleteTimings error:{}", response.getMsg());
            return response;
        }


        // update schedule
        scheduleService.updateUuidByUuid(oldUuid, newUuid);
        // update timer
        timerService.updateUuidByUuid(oldUuid, newUuid);
        // update away
        awayService.updateUuidByUuid(oldUuid, newUuid);
        return response;
    }

    @Override
    public VBMPResponse deleteByUuid(String uuid) {
        logger.info("invoke deleteTimings request:{}", uuid);
        VBMPResponse response = new VBMPResponse();
        if (MyStringUtils.isNullData(uuid)) {
            response.setCode(ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            response.setMsg(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            logger.error("invoke deleteTimings error:{}", response.getMsg());
            return response;
        }

        // delete schedule
        scheduleService.deleteScheduleByUUID(uuid);
        // delete timer
        timerService.deleteTimerByUuid(uuid);
        // delete away
        awayService.deleteTimerByUuid(uuid);

        return response;
    }
}
