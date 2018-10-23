package com.etekcity.vbmp.common.comm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.comm.dao.model.DeviceType;
import com.etekcity.vbmp.common.comm.service.DeviceTimingService;
import com.etekcity.vbmp.common.constant.CommonConstant;
import com.etekcity.vbmp.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @ClassName DeviceTimingServiceImpl
 * @Description
 * @Author Ericer
 * @Date 09-26 下午2:10
 **/
@Service
@Slf4j
public class DeviceTimingServiceImpl implements DeviceTimingService {
    @Value("${device.timing.url}")
    private String timingUrl;
    @Autowired
    private HttpClientService httpClientService;

    /**
     * @Author Ericer
     * @Description 根据uuid删除设备
     * @Date 下午3:50 18-9-26
     * @Param [uuid]
     * @return void
     **/
    @Override
    public void deleteTimingAllByUuid(String uuid){
        Assert.hasLength(uuid, "deleteTimingAllByUuid 接口参数uuid为空");
        log.info(String.format("调用deleteTimingAllByUuid接口,UUID:%s", uuid));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uuid", uuid);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Content-Type", "application/json;charset=utf-8");
        requestHeaders.setAcceptCharset(Arrays.asList(Charset.forName("utf-8")));
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonObject.toJSONString(), requestHeaders);
        String responseJsonStr = httpClientService.commonRequest(timingUrl.concat("/deleteByUuid"), HttpMethod.POST, requestEntity, String.class);
        JSONObject jsonObjectResponse = JSONObject.parseObject(responseJsonStr);
        if(jsonObjectResponse.getInteger("code") != CommonConstant.COMMON_SUCCESS){
            throw new ServiceException(jsonObjectResponse.getInteger("code"), jsonObjectResponse.getString("msg"));
        }
    }

}
