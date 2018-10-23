package com.etekcity.vbmp.common.router.service;

import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.comm.dto.DeviceRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @ClassName RouterService
 * @Description
 * @Author Ericer
 * @Date 09-14 上午11:04
 **/
public interface RouterService {

    VBMPResponse deleteDevice(DeviceRequest deviceRequest);

    void devRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);

    void changeStatusOutlet7A(JSONObject jsonObject, HttpServletResponse httpServletResponse);

    Integer changeCommonDeviceStatus(JSONObject requestJsonObject);
}
