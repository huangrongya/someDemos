package com.etekcity.vbmp.timing.common.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.timing.common.bean.DeviceType;
import com.etekcity.vbmp.timing.common.redis.RedisService;
import com.etekcity.vbmp.timing.common.service.DeviceInfoService;
import com.etekcity.vbmp.timing.common.service.DeviceTypeService;
import com.etekcity.vbmp.timing.constant.ErrorConstant;
import com.etekcity.vbmp.timing.exception.ServiceException;
import com.etekcity.vbmp.timing.util.MyStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
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
    private DeviceInfoService deviceInfoService;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Value("${device.ctrl.url}")
    private String url;

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

    public JSONObject getDeviceStatus(String uuid) {
        return request(new String[]{uuid}, uuid, "getDeviceStatus", HttpMethod.PUT);
    }

    public JSONObject request(Object msg, String uuid, String identify, HttpMethod method) {
        DeviceType deviceType = deviceTypeService.findDeviceTypeByModel(deviceInfoService.findDeviceByUuid(uuid).getDeviceType());
        if (deviceType == null || deviceType.getId() == null) {
            JSONObject res = new JSONObject();
            res.put("code", ErrorConstant.ERR_DEVICE_NOT_EXIST);
            res.put("msg", ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
            return res;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);
        JSONObject json = new JSONObject();
        json.put("uuid", uuid);
        json.put("msg", msg);
        json.put("configModel", deviceType.getConfigModel());
        json.put("identify", identify);
        HttpEntity<String> httpEntity = new HttpEntity<>(json.toJSONString(), headers);
        String response = commonRequest(url, method, httpEntity, String.class);
        log.info(JSON.toJSONString(response));
        if (MyStringUtils.isNullData(response)) {
            return new JSONObject();
        }
        return JSONObject.parseObject(response);
    }
}
