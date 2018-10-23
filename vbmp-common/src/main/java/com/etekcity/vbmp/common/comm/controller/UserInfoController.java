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
import com.etekcity.vbmp.common.comm.dto.UserInfoByUidRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoResponse;
import com.etekcity.vbmp.common.comm.dto.UserLangInfoRequest;
import com.etekcity.vbmp.common.comm.service.UserInfoService;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.filter.aop.Calibration;

/**
 * @author puyol
 */
@RestController
@RequestMapping("vbmp/common/userInfo/")
@Slf4j
public class UserInfoController {

    @Autowired
    UserInfoService userInfoService;


    @PostMapping("byUser")
    //@Calibration(fields = {"uuid"}, checkToken = true)
    public UserInfoResponse getUserInfoByUser(@RequestBody String json) {
        UserInfoRequest request = JSONObject.parseObject(json, UserInfoRequest.class);
        log.info("invoke getUserInfoByUser request: {}", JSON.toJSONString(request));
        UserInfoResponse response = userInfoService.getUserInfoByUser(request);
        log.info("invoke getUserInfoByUser response: {}", JSON.toJSONString(response));
        return response;
    }

    @PostMapping("byUid")
    //@Calibration(fields = {"uuid"}, checkToken = true)
    public UserInfoResponse getUserInfoByUid(@RequestBody String json) {
        UserInfoByUidRequest request = JSONObject.parseObject(json, UserInfoByUidRequest.class);
        log.info("invoke getUserInfoByUid request: {}", JSON.toJSONString(request));
        UserInfoResponse response = userInfoService.getUserInfoByUid(request);
        log.info("invoke getUserInfoByUid response: {}", JSON.toJSONString(response));
        return response;
    }

    @PostMapping("userLangInfo")
    //@Calibration(fields = {"uuid"}, checkToken = true)
    public UserInfoResponse getUserLangInfo(@RequestBody String json) {
        UserLangInfoRequest request = JSONObject.parseObject(json, UserLangInfoRequest.class);
        log.info("invoke getUserLangInfo request: {}", JSON.toJSONString(request));
        UserInfoResponse response = userInfoService.getUserLangInfo(request);
        log.info("invoke getUserLangInfo response: {}", JSON.toJSONString(response));
        return response;
    }

    
    @PostMapping("syncUserDivice")
    //@Calibration(fields = {"uuid"}, checkToken = true)
    public VBMPResponse syncUserDivice(@RequestBody String json) {
    	UserInfoRequest request = JSONObject.parseObject(json, UserInfoRequest.class);
    	log.info("invoke sendPowerOrElectrical request: {}", JSON.toJSONString(request));
    	VBMPResponse response = userInfoService.syncUserDivice(request);
    	log.info("invoke sendPowerOrElectrical response: {}", JSON.toJSONString(response));
    	return response;
    }

}
