/**
 *
 */
package com.etekcity.vbmp.common.comm.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.comm.dto.SendFcmPowerRequest;
import com.etekcity.vbmp.common.comm.dto.SendFcmRestRequest;
import com.etekcity.vbmp.common.comm.dto.SendFcmShareRequest;
import com.etekcity.vbmp.common.comm.service.SendFcmUserService;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.filter.aop.Calibration;

/**
 * @author larry
 */
@RestController
@RequestMapping("vbmp/common/fcmsend/")
@Slf4j
public class FcmSendController {

    @Autowired
    SendFcmUserService sendFcmUserService;

    @PostMapping("shardSend")
    @Calibration(fields = {"uuid","sharedPeopleId","msgKey","modelName"}, checkToken = true)
    public VBMPResponse sendFcmShard(@RequestBody String json) {
        SendFcmShareRequest request = JSONObject.parseObject(json, SendFcmShareRequest.class);
        log.info("invoke sendFcmShard request: {}", JSON.toJSONString(request));
        VBMPResponse response = sendFcmUserService.sendFcmUserAddOrDel(request);
        log.info("invoke sendFcmShard response: {}", JSON.toJSONString(response));
        return response;
    }

    @PostMapping("resetSend")
    @Calibration(fields = {"uuid","sharedPeopleIds","msgKey","modelName"}, checkToken = true)
    public VBMPResponse sendFcmRest(@RequestBody String json) {
        SendFcmRestRequest request = JSONObject.parseObject(json, SendFcmRestRequest.class);
        log.info("invoke sendFcmRest request: {}", JSON.toJSONString(request));
        VBMPResponse response = sendFcmUserService.sendFcmRest(request);
        log.info("invoke sendFcmRest response: {}", JSON.toJSONString(response));
        return response;
    }

    @PostMapping("powerSend")
    @Calibration(fields = {"uuid","msgKey","modelName"}, checkToken = true)
    public VBMPResponse sendPowerOrElectrical(@RequestBody String json) {
    	SendFcmPowerRequest request = JSONObject.parseObject(json, SendFcmPowerRequest.class);
        log.info("invoke sendPowerOrElectrical request: {}", JSON.toJSONString(request));
        VBMPResponse response = sendFcmUserService.sendFcmPower(request);
        log.info("invoke sendPowerOrElectrical response: {}", JSON.toJSONString(response));
        return response;
    }
    


    

}
