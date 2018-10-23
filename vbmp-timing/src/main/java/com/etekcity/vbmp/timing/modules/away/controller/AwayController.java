package com.etekcity.vbmp.timing.modules.away.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.timing.common.VBMPResponse;
import com.etekcity.vbmp.timing.filter.aop.ApplicationResponse;
import com.etekcity.vbmp.timing.filter.aop.Calibration;
import com.etekcity.vbmp.timing.modules.away.bean.AwayRequest;
import com.etekcity.vbmp.timing.modules.away.service.AwayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/vbmp/timing/away")
public class AwayController {
    private Logger logger = LoggerFactory.getLogger(AwayController.class);

    @Autowired
    AwayService awayService;

    @PostMapping("/addAway")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "timeZone", "startTime", "endTime", "repeat", "uuid"},
            checkToken = true, checkDevice = true, checkOnline = true)
    public VBMPResponse addAway(@RequestBody String req) throws ParseException {
        logger.info("invoke addAway request:{}", req);
        VBMPResponse response = awayService.addAway(JSONObject.parseObject(req, AwayRequest.class));
        logger.info("invoke addAway response:{}", JSON.toJSON(response));
        return response;
    }

    @DeleteMapping("/deleteAway")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "awayId"},
            checkToken = true, checkOnline = true)
    public VBMPResponse deleteAway(@RequestBody String req) {
        logger.info("invoke deleteAway request:{}", req);
        VBMPResponse response = awayService.deleteAway(JSONObject.parseObject(req, AwayRequest.class));
        logger.info("invoke deleteAway response:{}", JSON.toJSON(response));
        return response;
    }

    @PutMapping("/updateAway")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "timeZone", "startTime", "endTime", "repeat", "awayId", "awayStatus"},
            checkToken = true, checkOnline = true)
    public VBMPResponse updateAway(@RequestBody String req) {
        logger.info("invoke updateAway request:{}", req);
        VBMPResponse response = awayService.updateAway(JSONObject.parseObject(req, AwayRequest.class));
        logger.info("invoke updateAway response:{}", JSON.toJSON(response));
        return response;
    }

    @PutMapping("/updateAwayStatus")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "timeZone", "awayStatus", "awayId"},
            checkToken = true, checkOnline = true)
    public VBMPResponse updateAwayStatus(@RequestBody String req) {
        logger.info("invoke updateAwayStatus request:{}", req);
        VBMPResponse response = awayService.updateAwayStatus(JSONObject.parseObject(req, AwayRequest.class));
        logger.info("invoke updateAwayStatus response:{}", JSON.toJSONString(response));
        return response;
    }

    @PostMapping("/getAways")
    @ApplicationResponse
    @Calibration(fields = {"accountId", "token", "timeZone", "uuid"},
            checkToken = true, checkDevice = true, checkOnline = true)
    public VBMPResponse getAways(@RequestBody String req) throws ParseException {
        logger.info("invoke getAways request:{}", req);
        VBMPResponse response = awayService.getAways(JSONObject.parseObject(req, AwayRequest.class));
        logger.info("invoke getAways response:{}", JSON.toJSON(response));
        return response;
    }

}
