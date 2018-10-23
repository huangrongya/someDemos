package com.etekcity.vbmp.timing.modules.schedule.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.timing.common.VBMPResponse;
import com.etekcity.vbmp.timing.constant.ErrorConstant;
import com.etekcity.vbmp.timing.filter.aop.ApplicationResponse;
import com.etekcity.vbmp.timing.filter.aop.Calibration;
import com.etekcity.vbmp.timing.modules.schedule.bean.ScheduleRequest;
import com.etekcity.vbmp.timing.modules.schedule.service.ScheduleService;
import com.etekcity.vbmp.timing.util.MyStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vbmp/timing/schedule")
public class ScheduleController {

    private Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    @Autowired
    ScheduleService scheduleService;

    @PostMapping("/addSchedule")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "uuid", "sunTime", "startTime", "startState", "timeZone", "repeat"},
            checkToken = true, checkDevice = true, checkOnline = true)
    public VBMPResponse addSchedule(@RequestBody String req) throws Exception {
        logger.info("invoke addSchedule params:" + req);
        ScheduleRequest request = JSONObject.parseObject(req, ScheduleRequest.class);
        if (!MyStringUtils.isNullData(request.getEndTime()) && MyStringUtils.isNullData(request.getEndState())) {
            logger.error("传了结束时间必须传结束Action");
            return new VBMPResponse(ErrorConstant.ERR_REQUEST_PARAM_EMPTY, ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
        }
        if ("1".equals(request.getSunTime())) { // 使用日出日落时间
            if (MyStringUtils.isNullData(request.getLongitude(), request.getLatitude())) {
                logger.error("SunTime为1时必须传经纬度");
                return new VBMPResponse(ErrorConstant.ERR_REQUEST_PARAM_EMPTY, ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            }
        }
        if (request.getStartTime().equals(request.getEndTime())) {
            logger.error("开始时间和结束时间不能相同");
            return new VBMPResponse(ErrorConstant.ERR_INVALID_PARAM_FORMAT, ErrorConstant.ERR_INVALID_PARAM_FORMAT_MSG);
        }
        VBMPResponse response = scheduleService.addSchedule(request);
        logger.info("增加schedule返回:{}", JSON.toJSONString(response));
        return response;
    }

    @DeleteMapping("/deleteSchedule")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "scheduleId", "uuid"},
            checkToken = true, checkOnline = true)
    public VBMPResponse deleteSchedule(@RequestBody String req) {
        ScheduleRequest request = JSONObject.parseObject(req, ScheduleRequest.class);
        logger.info("invoke deleteSchedule params:" + req);
        VBMPResponse response = scheduleService.deleteSchedule(request);
        return response;
    }

    @PutMapping("/updateSchedule")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "uuid", "sunTime", "startTime", "startState", "scheduleState", "timeZone", "repeat", "scheduleId"},
            checkToken = true, checkOnline = true)
    public VBMPResponse updateSchedule(@RequestBody String req) throws Exception {
        ScheduleRequest request = JSON.parseObject(req, ScheduleRequest.class);
        logger.info("invoke updateSchedule params:" + JSON.toJSONString(request));
        if (!MyStringUtils.isNullData(request.getEndTime()) && MyStringUtils.isNullData(request.getEndState())) {
            logger.error("传了结束时间必须传结束Action");
            return new VBMPResponse(ErrorConstant.ERR_REQUEST_PARAM_EMPTY, ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
        }
        if ("1".equals(request.getSunTime())) { // 使用日出日落时间
            if (MyStringUtils.isNullData(request.getLongitude(), request.getLatitude())) {
                logger.error("SunTime为1时必须传经纬度");
                return new VBMPResponse(ErrorConstant.ERR_REQUEST_PARAM_EMPTY, ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            }
        }
        if (request.getStartTime().equals(request.getEndTime())) {
            logger.error("开始时间和结束时间不能相同");
            return new VBMPResponse(ErrorConstant.ERR_INVALID_PARAM_FORMAT, ErrorConstant.ERR_INVALID_PARAM_FORMAT_MSG);
        }
        VBMPResponse response = scheduleService.updateSchedule(request);
        return response;
    }

    @PutMapping("/updateScheduleState")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "scheduleId", "scheduleState", "timeZone"},
            checkToken = true, checkOnline = true)
    public VBMPResponse updateScheduleState(@RequestBody String req) throws Exception {
        ScheduleRequest request = JSON.parseObject(req, ScheduleRequest.class);
        logger.info("invoke updateScheduleState request:" + req);
        if (!"0".equals(request.getScheduleState()) && !"1".equals(request.getScheduleState())) {
            logger.error("设备Timer状态只能是0和1");
            return new VBMPResponse(ErrorConstant.ERR_INVALID_PARAM_FORMAT, ErrorConstant.ERR_INVALID_PARAM_FORMAT_MSG);
        }
        VBMPResponse response = scheduleService.updateScheduleState(request);
        logger.info("invoke updateTimerStatus result:" + JSON.toJSONString(response));
        return response;
    }

    @PostMapping("/getSchedules")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "uuid", "timeZone"},
            checkToken = true, checkDevice = true, checkOnline = true)
    public VBMPResponse getSchedules(@RequestBody String req) {
        logger.info("invoke getSchedules params:" + req);
        VBMPResponse response = scheduleService.getSchedules(JSON.parseObject(req, ScheduleRequest.class));
        logger.info("获取schedules返回:{}", JSON.toJSONString(response));
        return response;
    }
}
