package com.etekcity.vbmp.common.router.service;

import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.router.dto.GetConrtolRequest;
import com.etekcity.vbmp.common.router.dto.RequestBaseInfo;

import java.util.List;
import java.util.Map;

/**
 * @ClassName DeviceControlService
 * @Description
 * @Author Ericer
 * @Date 09-17 下午3:19
 **/
public interface DeviceControlService {
    JSONObject getDeviceStatus(String uuid);

    JSONObject removeDevice(String uuid);

    JSONObject deleteVdmpUserData(String uuid, String userid, List<String> cids);

    JSONObject restoreDeviceState(String uuid);

    JSONObject delUserDataOffline(String uuid);

    JSONObject delDeviceUserData(String uuid);

    Integer change7aOutletStatus(RequestBaseInfo devicesRequest, String cid, String deviceAction);

    Integer changeCommonDeviceStatus(GetConrtolRequest getConrtolRequest);

    Integer changeCommonDeviceStatus(JSONObject requestJsonObject);
}
