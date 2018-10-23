package com.etekcity.vbmp.common.comm.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.comm.dto.*;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.config.redis.RedisService;
import com.etekcity.vbmp.common.comm.service.ShareService;
import com.etekcity.vbmp.common.filter.aop.Calibration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("vbmp/common/share")
@Slf4j
public class ShareController {

    @Autowired
    ShareService shareService;

    @Autowired
    RedisService redisService;

    @PostMapping("/add")
    @Calibration(fields = {"uuid"},checkToken = true)
    public VBMPResponse addSharePeople(@RequestBody String json) {
        AddSharePeopleRequest request = JSONObject.parseObject(json, AddSharePeopleRequest.class);
        VBMPResponse response = shareService.addSharePeople(request);
        return response;
    }

    @DeleteMapping("/delete")
    @Calibration(fields = {"uuid", "sharedPeopleId"},checkToken = true)
    public VBMPResponse deleteSharePeople(@RequestBody String json) {
        DeleteSharePeopleRequest request = JSONObject.parseObject(json, DeleteSharePeopleRequest.class);
        VBMPResponse response = shareService.deleteSharePeople(request);
        return response;
    }

    @PostMapping("/shares")
    @Calibration(fields = {"uuid"},checkToken = true)
    public VBMPResponse querySharePeople(@RequestBody String json) {
        SharePeopleRequest request = JSONObject.parseObject(json, SharePeopleRequest.class);
        SharePeopleResponse response = shareService.querySharePeople(request);
        return response;
    }

    @PostMapping("/history")
    @Calibration(fields = {"uuid", "num"},checkToken = true)
    public VBMPResponse querySharePeopleHistory(@RequestBody String json) {
        ShareHistoryRequest request = JSONObject.parseObject(json, ShareHistoryRequest.class);
        SharePeopleResponse response = shareService.querySharePeopleHistory(request);
        return response;
    }

    @DeleteMapping("/history/delete")
    @Calibration(fields = {"uuid"},checkToken = true)
    public VBMPResponse deleteSharePeopleHistory(@RequestBody String json) {
        SharePeopleRequest request = JSONObject.parseObject(json, SharePeopleRequest.class);
        VBMPResponse response = shareService.deleteSharePeopleHistory(request);
        return response;
    }

    @PostMapping("/shares/uuid")
    @Calibration()
    public VBMPResponse querySharePeopleByUuid(@RequestBody String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        String uuid = jsonObject.getString("uuid");
        List<String> list = shareService.queryDeviceSharerListByUuid(uuid);
        AccountIdResponse response = new AccountIdResponse();
        response.setAccountIds(list);
        return response;
    }

    @DeleteMapping("/delete/uuid")
    @Calibration()
    public VBMPResponse deleteShareDataByUuid(@RequestBody String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        String uuid = jsonObject.getString("uuid");
        VBMPResponse response = shareService.deleteShareDataByUuid(uuid);
        return response;
    }

}
