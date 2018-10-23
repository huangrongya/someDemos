package com.etekcity.vbmp.common.comm.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.config.redis.RedisService;
import com.etekcity.vbmp.common.constant.CommonConstant;
import com.etekcity.vbmp.common.constant.ErrorConstant;
import com.etekcity.vbmp.common.comm.dao.model.DeviceType;
import com.etekcity.vbmp.common.exception.ServiceException;
import com.etekcity.vbmp.common.comm.dao.model.DeviceInfo;
import com.etekcity.vbmp.common.comm.service.DeviceService;
import com.etekcity.vbmp.common.comm.service.DeviceTypeService;
import com.etekcity.vbmp.common.utils.MyJsonUtils;
import com.etekcity.vbmp.common.utils.MyStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.List;

@Service
@Slf4j
public class HttpClientService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private RedisService redisService;

    @Value("${device.ctrl.url}")
    private String url;

    @Value("${device.updatefirmware.address}")
    private String deviceFirmware;

    public String commonRequestUtf8(String urlPath, HttpMethod httpMethod, HttpEntity<?> requestEntity, Class<String> responseType) {
        ResponseEntity<String> response = null;
        String result = null;
        try {
            // 添加utf-8支持
            List<HttpMessageConverter<?>> converterList = restTemplate.getMessageConverters();
            HttpMessageConverter<?> converter = new StringHttpMessageConverter();
            ((StringHttpMessageConverter) converter).setDefaultCharset(Charset.forName("UTF-8"));
            if (!(converterList.get(0) instanceof StringHttpMessageConverter)) {
                converterList.add(0, converter);
                restTemplate.setMessageConverters(converterList);
            }
            log.info("发送http请求url:{}, method:{}, request:{}", urlPath, httpMethod, JSONObject.toJSONString(requestEntity));
            response = restTemplate.exchange(urlPath, httpMethod, requestEntity, responseType);
            log.info("接收http请求response:{}", JSONObject.toJSONString(response));
        } catch (HttpStatusCodeException e) {
            log.info("发送http请求错误", e);
            result = e.getResponseBodyAsString();
        } catch (Exception e) {
            log.info("发送http请求错误", e);
            throw new ServiceException(ErrorConstant.ERR_THIRDPARTY_HTTP, ErrorConstant.ERR_THIRDPARTY_HTTP_MSG);
        }
        JSONObject jsonObject;
        // 打印header错误码
        if (StringUtils.hasText(result) && (jsonObject = JSONObject.parseObject(result)) != null) {
            log.warn("请求出错,错误码：{}", result);
            if (jsonObject != null) {
                jsonObject = jsonObject.getJSONObject("error");
                throw new ServiceException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
            }
        }
        if (response != null) {
            result = response.getBody();
        }
        if (result == null) {
            result = "";
        }
        return result;
    }

    public String commonRequest(String url, HttpMethod httpMethod, HttpEntity<?> requestEntity, Class<String> responseType) {
        ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, requestEntity, responseType);
        if (response == null || MyStringUtils.isNullData(response.getBody())) {
            return "";
        }
        return response.getBody();
    }

    public String commonHttpsRequest(String urlPath, HttpMethod httpMethod, HttpEntity<?> requestEntity, Class<String> responseType) {
        ResponseEntity<String> response = null;
        String responseString = null;
        try {
            log.info("发送http请求url:{}, method:{}, request:{}", urlPath, httpMethod, JSONObject.toJSONString(requestEntity));
            response = restTemplate.exchange(urlPath, httpMethod, requestEntity, responseType);
            log.info("接收http请求response:{}", JSONObject.toJSONString(response));
        } catch (HttpStatusCodeException e) {
            log.info("发送http请求错误", e);
            responseString = e.getResponseBodyAsString();
        } catch (Exception e) {
            log.info("发送http请求错误", e);
            throw new ServiceException(ErrorConstant.ERR_HTTP_REQUEST, ErrorConstant.ERR_HTTP_REQUEST_MSG);
        }
        //打印header错误码
        if (response != null && response.getHeaders().containsKey("statusCode")) {
            log.warn("请求返回码：{}", JSONObject.toJSONString(response.getHeaders().get("statusCode")));
        }
        if (response != null) {
            responseString = response.getBody();
        }
        if (responseString == null) {
            responseString = "";
        }
        return responseString;
    }

    public void updateDeviceFirmware(String uuid, String newVersion, String deviceType) {
        DeviceType dt = deviceTypeService.getDeviceType(deviceType);
        if (dt == null || dt.getId() == null) {
            log.error("deviceType:{}不存在", deviceType);
            return;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);
        String version = "v".concat(newVersion);
        JSONObject json = new JSONObject();
        json.put("uuid", uuid);
        json.put("configModel", dt.getConfigModel());
        json.put("identify", "updateFirmware");
        json.put("msg", new String[]{newVersion, deviceFirmware.concat(dt.getFirmType()).concat("/").concat(version).concat("/"), (System.currentTimeMillis() / 1000) + ""});
        HttpEntity<String> httpEntity = new HttpEntity<>(json.toJSONString(), headers);
        String response = commonRequest(url, HttpMethod.PUT, httpEntity, String.class);
        JSONObject jsonResponse = JSONObject.parseObject(response);
        log.debug("调用changeDeviceStatus接口,返回:{}", JSONObject.toJSONString(jsonResponse));
        //设备不在线
        if (jsonResponse != null && (jsonResponse.getInteger("code") == 30000006
                || CommonConstant.COMMON_CONNECTION_STATUS_OFFLINE.equals(MyJsonUtils.getJsonInfo(jsonResponse, "data", "payload", "state", "reported", "connectionStatus")))) {
            throw new ServiceException(ErrorConstant.ERR_CONTROLLER_OFFLINE, ErrorConstant.ERR_CONTROLLER_OFFLINE_MSG);
        } else if (jsonResponse != null && (jsonResponse.getInteger("code") == 30000003)) { // 控制设备超时
            throw new ServiceException(ErrorConstant.ERR_CONTROLLER_NO_RESPONSE, ErrorConstant.ERR_CONTROLLER_NO_RESPONSE_MSG);
        } else if (jsonResponse == null || jsonResponse.getInteger("code") != CommonConstant.COMMON_SUCCESS) {
            throw new ServiceException(jsonResponse.getInteger("code"), jsonResponse.getString("msg"));
        } else if (jsonResponse == null) {
            throw new ServiceException(ErrorConstant.ERR_VDMP_REQUEST_FORMAT, ErrorConstant.ERR_VDMP_REQUEST_FORMAT_MSG);
        }
    }

    public void updateFirmwareVersion(String uuid, String version) {
        DeviceInfo device = deviceService.queryDeviceByUuid(uuid);
        Assert.notNull(device, "uuid对应的设备不存在");
        DeviceType deviceType = deviceTypeService.getDeviceTypeByUuid(uuid);
        Assert.notNull(deviceType, "devicetype对应的设备类型在DeviceTypeTable为空");
        // 版本号不相同
        if (!version.equals(device.getCurrentFirmVersion())) {
            device.setCurrentFirmVersion(version);
            deviceService.updateByPrimaryKey(device);
            String redisUuidDKey = CommonConstant.REDIS_KEY_DEVICE_UUID.concat(uuid);
            String objectField = CommonConstant.UUID_DEVICE_OBJECT;
            redisService.removeMapField(redisUuidDKey, objectField);
        }
    }

    /**
     * 请求device-control模块
     *
     * @param msg      数组
     * @param uuid     UUID
     * @param identify 方法名
     * @return JSONObject
     */
    public JSONObject request(String[] msg, String uuid, String identify) {
        DeviceType deviceType = deviceTypeService.getDeviceTypeByUuid(uuid);
        if (deviceType == null || deviceType.getId() == null) {
            JSONObject response = new JSONObject();
            response.put("code", ErrorConstant.ERR_DEVICE_NOT_EXIST);
            response.put("msg", ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
            return response;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);
        JSONObject json = new JSONObject();
        json.put("msg", msg);
        json.put("uuid", uuid);
        json.put("identify", identify);
        json.put("configModel", deviceType.getConfigModel());
        HttpEntity<String> httpEntity = new HttpEntity<>(json.toJSONString(), headers);
        String response = commonRequest(url, HttpMethod.PUT, httpEntity, String.class);
        log.info("调用设备控制:{}方法返回:{}", identify, JSON.toJSONString(response));
        if (MyStringUtils.isNullData(response)) {
            JSONObject result = new JSONObject();
            result.put("code", ErrorConstant.ERR_VDMP_REQUEST_FORMAT);
            result.put("msg", ErrorConstant.ERR_VDMP_REQUEST_FORMAT_MSG);
            return result;
        }
        return JSONObject.parseObject(response);
    }
}
