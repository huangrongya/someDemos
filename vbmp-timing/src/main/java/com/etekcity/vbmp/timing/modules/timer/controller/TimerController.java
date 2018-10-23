package com.etekcity.vbmp.timing.modules.timer.controller;

import com.alibaba.fastjson.JSON;
import com.etekcity.vbmp.timing.common.VBMPResponse;
import com.etekcity.vbmp.timing.constant.ErrorConstant;
import com.etekcity.vbmp.timing.filter.aop.ApplicationResponse;
import com.etekcity.vbmp.timing.filter.aop.Calibration;
import com.etekcity.vbmp.timing.modules.timer.bean.TimerRequest;
import com.etekcity.vbmp.timing.modules.timer.service.TimerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vbmp/timing/timer")
public class TimerController {

    private Logger logger = LoggerFactory.getLogger(TimerController.class);

    @Autowired
    TimerService timerService;

    @PostMapping("addTimer")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "counterTime", "action", "uuid"},
            checkToken = true, checkDevice = true, checkOnline = true)
    public VBMPResponse addTimer(@RequestBody String req) {
        TimerRequest request = JSON.parseObject(req, TimerRequest.class);
        logger.info("invoke addTime request:" + req);
        if (!"on".equals(request.getAction()) && !"off".equals(request.getAction())) {
            logger.error("请求参数错误，status只能是1和0");
            return new VBMPResponse(ErrorConstant.ERR_REQUEST_INVALID_PARAM, ErrorConstant.ERR_REQUEST_INVALID_PARAM_MSG);
        }
        if (Integer.valueOf(request.getCounterTime()) <= 0) {
            logger.error("Timer倒计时时间必须大于0");
            return new VBMPResponse(ErrorConstant.ERR_TIMER_COUNT_MAX, ErrorConstant.ERR_TIMER_COUNT_MAX_MSG);
        }
        int maxCount = 46799;
        if (Integer.valueOf(request.getCounterTime()) > maxCount) {
            logger.error("Timer倒计时时间必须小于13个小时");
            return new VBMPResponse(ErrorConstant.ERR_TIMER_COUNT_MAX, ErrorConstant.ERR_TIMER_COUNT_MAX_MSG);
        }
        return timerService.addTimer(request);
    }

    @DeleteMapping("deleteTimer")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "timerId"},
            checkToken = true, checkOnline = true)
    public VBMPResponse deleteTimer(@RequestBody String req) {
        TimerRequest request = JSON.parseObject(req, TimerRequest.class);
        logger.info("invoke deleteTimer request:" + req);
        return timerService.deleteTimer(request.getTimerId());
    }

    @PutMapping("updateTimer")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "counterTime", "timerId", "action", "status"},
            checkToken = true, checkOnline = true)
    public VBMPResponse updateTimer(@RequestBody String req) {
        TimerRequest request = JSON.parseObject(req, TimerRequest.class);
        logger.info("invoke updateTimer request:" + req);
        if (!"on".equals(request.getAction()) && !"off".equals(request.getAction())) {
            logger.error("请求参数错误，action只能是on和off");
            return new VBMPResponse(ErrorConstant.ERR_REQUEST_INVALID_PARAM, ErrorConstant.ERR_REQUEST_INVALID_PARAM_MSG);
        }
        if (!"0".equals(request.getStatus()) && !"1".equals(request.getStatus())) {
            logger.error("请求参数错误，status只能是1和0");
            return new VBMPResponse(ErrorConstant.ERR_REQUEST_INVALID_PARAM, ErrorConstant.ERR_REQUEST_INVALID_PARAM_MSG);
        }
        if (Integer.valueOf(request.getCounterTime()) <= 0) {
            logger.error("Timer倒计时时间必须大于0");
            return new VBMPResponse(ErrorConstant.ERR_TIMER_COUNT_MAX, ErrorConstant.ERR_TIMER_COUNT_MAX_MSG);
        }
        // validate counterTimer <= 12:59:59 (46799)
        int maxCount = 46799;
        if (Integer.valueOf(request.getCounterTime()) > maxCount) {
            logger.error("Timer倒计时时间必须小于13个小时");
            return new VBMPResponse(ErrorConstant.ERR_TIMER_COUNT_MAX, ErrorConstant.ERR_TIMER_COUNT_MAX_MSG);
        }
        return timerService.updateTimer(request);
    }

    @PutMapping("/updateTimerState")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "timerId", "status"},
            checkToken = true, checkOnline = true)
    public VBMPResponse updateTimerStatus(@RequestBody String req) {
        TimerRequest request = JSON.parseObject(req, TimerRequest.class);
        logger.info("invoke updateTimerStatus request:" + req);
        if (!"0".equals(request.getStatus()) && !"1".equals(request.getStatus())) {
            logger.error("设备Timer状态只能是0和1");
            return new VBMPResponse(ErrorConstant.ERR_INVALID_PARAM_FORMAT, ErrorConstant.ERR_INVALID_PARAM_FORMAT_MSG);
        }
        VBMPResponse response = timerService.updateTimerStatus(request);
        logger.info("invoke updateTimerStatus result:" + JSON.toJSONString(response));
        return response;
    }

    @PostMapping("/getTimers")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "uuid"},
            checkToken = true, checkDevice = true, checkOnline = true)
    public VBMPResponse getTimers(@RequestBody String req) {
        TimerRequest request = JSON.parseObject(req, TimerRequest.class);
        logger.info("invoke getTimers request:{}", req);
        VBMPResponse response = timerService.getTimers(request);
        logger.info("invoke getTimers response:{}", JSON.toJSONString(response));
        return response;
    }


    @PostMapping("/deleteByUuid")
    public VBMPResponse deleteByUuid(@RequestBody String req) {
        String uuid = JSON.parseObject(req).getString("uuid");
        VBMPResponse response = timerService.deleteTimerByUuid(uuid);
        return response;
    }

}
