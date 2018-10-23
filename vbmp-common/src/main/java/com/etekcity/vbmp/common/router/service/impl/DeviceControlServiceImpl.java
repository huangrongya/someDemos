package com.etekcity.vbmp.common.router.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.comm.dao.model.DeviceInfo;
import com.etekcity.vbmp.common.comm.dao.model.DeviceType;
import com.etekcity.vbmp.common.comm.service.DeviceService;
import com.etekcity.vbmp.common.comm.service.DeviceTypeService;
import com.etekcity.vbmp.common.comm.service.impl.HttpClientService;
import com.etekcity.vbmp.common.constant.CommonConstant;
import com.etekcity.vbmp.common.constant.ErrorConstant;
import com.etekcity.vbmp.common.exception.ServiceException;
import com.etekcity.vbmp.common.router.dto.GetConrtolRequest;
import com.etekcity.vbmp.common.router.dto.RequestBaseInfo;
import com.etekcity.vbmp.common.router.service.DeviceControlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.util.*;

/**
 * @ClassName DeviceControlServiceImpl
 * @Description
 * @Author Ericer
 * @Date 09-17 下午3:19
 **/
@Service
@Slf4j
public class DeviceControlServiceImpl implements DeviceControlService {
    @Autowired
    private HttpClientService httpClientService;
    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceTypeService deviceTypeService;
    @Value("${device.ctrl.url}")
    private String url;
    @Value("${outlet7a.domin.url}")
    private String outlet7AAddress;

    private String getDeviceStatus = "getDeviceStatus";
    private String removeDevice = "removeDevice";
    private String deleteVdmpUserData = "deleteVdmpUserData";
    private String delDevice = "delDevice";
    private String delUserData = "delUserData";


    @Override
    public JSONObject getDeviceStatus(String uuid) {
        Map<String, Object> bodyParam = new HashMap<>();
        bodyParam.put("uuid", uuid);
        GetConrtolRequest getConrtolRequest = new GetConrtolRequest();
        getConrtolRequest.setIdentify(getDeviceStatus);
        DeviceType deviceType = queryDeviceType(uuid);
        getConrtolRequest.setConfigModel(deviceType.getConfigModel());
        getConrtolRequest.setUuid(uuid);
        return commonVbmpControl(uuid, null, getDeviceStatus, null);
    }


    private DeviceType queryDeviceType(String uuid) {
        DeviceInfo deviceInfo = deviceService.queryDeviceByUuid(uuid);
        if (deviceInfo == null || deviceInfo.getId() == null) {
            throw new ServiceException(ErrorConstant.ERR_DEVICE_NOT_EXIST, ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
        }
        DeviceType deviceType = deviceTypeService.getDeviceType(deviceInfo.getDeviceType());
        if (deviceType == null || deviceType.getId() == null) {
            throw new ServiceException(ErrorConstant.ERR_DEVICE_NOT_EXIST, ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
        }
        return deviceType;
    }


    private JSONObject commonVbmpControl(String uuid, Object[] bodyParam, String identify, Map<String, Object> headerParam) {
        Assert.hasLength(uuid, "getDeviceStatus 接口参数uuid为空");
        log.debug(String.format("调用getDeviceStatus接口,UUID:%s", uuid));

        DeviceType deviceType = queryDeviceType(uuid);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uuid", uuid);
        jsonObject.put("identify", identify);
        jsonObject.put("configModel", deviceType.getConfigModel());
        if(bodyParam != null){
            jsonObject.put("msg", bodyParam);
        }

        if(headerParam != null){
            jsonObject.put("paramMap", headerParam);
        }

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Content-Type", "application/json;charset=utf-8");
        requestHeaders.setAcceptCharset(Arrays.asList(Charset.forName("utf-8")));
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonObject.toJSONString(), requestHeaders);

        String path = url;
        String responseJsonStr = httpClientService.commonHttpsRequest(path, HttpMethod.PUT, requestEntity, String.class);
        log.info("控制设备返回结果：".concat(responseJsonStr));
        JSONObject jsonObjectResponse = JSONObject.parseObject(responseJsonStr);

        return jsonObjectResponse;
    }

    /**
     * 删除设备
     *
     * @param uuid
     * @return
     */
    @Override
    public JSONObject removeDevice(String uuid) {
        Assert.hasLength(uuid, "removeDevice 接口参数uuid为空");
        return commonVbmpControl(uuid, null, removeDevice, null);
    }

    /**
     * @return com.alibaba.fastjson.JSONObject
     * @Author Ericer
     * @Description 删除vdmp平台数据
     * @Date 下午4:55 18-9-18
     * @Param [uuid, userid, cids]
     **/
    @Override
    public JSONObject deleteVdmpUserData(String uuid, String userid, List<String> cids) {
        Assert.hasLength(uuid, "deleteVdmpUserData 接口参数uuid为空");
        log.debug("调用deleteVdmpUserData接口,UUID:{}", uuid);
        List<Object> bodyParam = new ArrayList<>();
        bodyParam.add(userid);
        bodyParam.add(cids);
        return commonVbmpControl(uuid, bodyParam.toArray(), deleteVdmpUserData, null);
    }


    /**
     * 重值设备(删除设备时调用)
     *
     * @param uuid
     * @return
     */
    @Override
    public JSONObject restoreDeviceState(String uuid) {
        return commonVbmpControl(uuid, null, delDevice, null);
    }

    /**
     * 删除离线设备
     *
     * @param uuid UUID
     * @return
     */
    @Override
    public JSONObject delUserDataOffline(String uuid) {
        Assert.hasLength(uuid, "restoreDeviceStateOffline 接口参数uuid为空");
        Map<String, Object> headerParam = new HashMap<>(1);
        headerParam.put("offline", "1");
        return commonVbmpControl(uuid, null, delUserData, headerParam);
    }

    /**
     * 删除除配网信息的其他信息 配网时调用
     *
     * @param uuid
     * @return
     */
    @Override
    public JSONObject delDeviceUserData(String uuid) {
        return commonVbmpControl(uuid, null, delUserData, null);
    }

    /**
     * @return java.lang.Integer
     * @Author Ericer
     * @Description 控制7A设备状态（googleHome使用）
     * @Date 下午4:36 18-9-18
     * @Param [devicesRequest, cid, deviceAction]
     **/
    @Override
    public Integer change7aOutletStatus(RequestBaseInfo devicesRequest, String cid, String deviceAction) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Content-Type", "application/json;charset=utf-8");
        requestHeaders.setAcceptCharset(Arrays.asList(Charset.forName("utf-8")));
        requestHeaders.set("tk", devicesRequest.getToken());
        requestHeaders.set("accountID", devicesRequest.getAccountID());
        HttpEntity<String> requestEntity = new HttpEntity<>("", requestHeaders);
        //转换状态对应, on/off
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(outlet7AAddress.concat("/v1/thirdparty/wifi-switch-1.3/".concat(cid).concat("/status/").concat(deviceAction)));
        String responseJsonStr = httpClientService.commonHttpsRequest(stringBuilder.toString(), HttpMethod.PUT, requestEntity, String.class);
        log.info("控制7A设备返回结果：".concat(responseJsonStr));
        JSONObject jsonObject = JSONObject.parseObject(responseJsonStr);
        if (jsonObject == null) {
            return 0;
        }
        Integer code = jsonObject.getJSONObject("error").getInteger("code");
        //按照googleHome返回   "11"：设备离线 "20": 设备无响应 "19":该用户没有此设备
        Integer returnCode = code;
        if (code == ErrorConstant.ERR_CONTROLLER_NO_RESPONSE) {
            returnCode = 20;
        } else if (code == ErrorConstant.ERR_CONTROLLER_OFFLINE) {
            returnCode = 11;
        } else if (code == ErrorConstant.ERR_USER_DONOT_OWN_DEVICE) {
            returnCode = 19;
        }
        return returnCode;
    }

    /**
     * @return java.lang.Integer
     * @Author Ericer
     * @Description 通用googlehome控制设备
     * @Date 下午4:36 18-9-18
     * @Param [uuid, operation, bodyParam]
     **/
    @Override
    public Integer changeCommonDeviceStatus(GetConrtolRequest getConrtolRequest) {
        Assert.hasLength(getConrtolRequest.getUuid(), "changeCommonDeviceStatus uuid不能为空");
        Assert.hasLength(getConrtolRequest.getIdentify(), "changeCommonDeviceStatus identify不能为空");
        Assert.hasLength(getConrtolRequest.getConfigModel(), "changeCommonDeviceStatus configModel不能为空");
        Assert.notNull(getConrtolRequest.getBodyParam(), "changeCommonDeviceStatus bodyParam不能为空");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uuid", getConrtolRequest.getUuid());
        jsonObject.put("identify", getConrtolRequest.getIdentify());
        jsonObject.put("configModel", getConrtolRequest.getConfigModel());
        jsonObject.put("msg", getConrtolRequest.getBodyParam().toArray());
        return changeCommonDeviceStatus(jsonObject);
    }


    /**
     * @return java.lang.Integer
     * @Author Ericer
     * @Description 通用googlehome控制设备
     * @Date 下午4:36 18-9-18
     * @Param [requestJsonObject]
     **/
    @Override
    public Integer changeCommonDeviceStatus(JSONObject requestJsonObject) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Content-Type", "application/json;charset=utf-8");
        requestHeaders.setAcceptCharset(Arrays.asList(Charset.forName("utf-8")));
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJsonObject.toJSONString(), requestHeaders);

        String path = url;
        String responseJsonStr = httpClientService.commonHttpsRequest(path, HttpMethod.PUT, requestEntity, String.class);
        log.info("控制设备返回结果：".concat(responseJsonStr));
        JSONObject jsonObjectResponse = JSONObject.parseObject(responseJsonStr);
        Integer code = jsonObjectResponse.getInteger("code");
        //按照googleHome返回   "11"：设备离线 "20": 设备无响应 "19":该用户没有此设备
        Integer returnCode = code;
        if (code == ErrorConstant.ERR_CONTROLLER_NO_RESPONSE) {
            returnCode = 20;
        } else if (code == ErrorConstant.ERR_CONTROLLER_OFFLINE) {
            returnCode = 11;
        } else if (code == CommonConstant.COMMON_SUCCESS) {
            returnCode = 0;
        }
        return returnCode;
    }
}
