package com.etekcity.vbmp.common.comm.service.impl;

import com.etekcity.vbmp.common.constant.ErrorConstant;
import com.etekcity.vbmp.common.exception.ServiceException;
import com.etekcity.vbmp.common.router.dao.mapper.DeviceControlModelMapper;
import com.etekcity.vbmp.common.router.dao.model.DeviceControlModel;
import com.etekcity.vbmp.common.comm.service.DeviceControlModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @ClassName DeviceControlModelServiceImpl
 * @Description
 * @Author Ericer
 * @Date 9-16 上午9:26
 **/

@Service
@Slf4j
public class DeviceControlModelServiceImpl implements DeviceControlModelService {
    @Autowired
    private DeviceControlModelMapper deviceControlModelMapper;
    @Autowired
    HttpClientService httpClientService;

    /**
     * @return com.etekcity.vbmp.device.conrtol.dto.ErrorResponse
     * @Author Ericer
     * @Description 发送vdmp消息
     * @Date 下午5:42 18-9-17
     * @Param [request]
     **/
    @Override
    public String getVdmpConrtolIdentifyByModel(String configModel, String operation) {
        Assert.hasText(configModel, "sendVdmpConrtolByModel configModel");
        Assert.hasText(operation, "sendVdmpConrtolByModel operation");

        DeviceControlModel deviceControlModelQuery = new DeviceControlModel();
        deviceControlModelQuery.setConfigModel(configModel);
        deviceControlModelQuery.setOperation(operation);
        DeviceControlModel deviceControlModel = getDeviceControl(deviceControlModelQuery);
        if (deviceControlModel == null) {
            throw new ServiceException(ErrorConstant.ERR_REQUEST_INVALID_DEVICE_CONTROL_MODEL_PARAM, ErrorConstant.ERR_REQUEST_INVALID_DEVICE_CONTROL_MODEL_PARAM_MSG);
        }

        String indentify = deviceControlModel.getIdentify();

        return indentify;
    }


    public DeviceControlModel getDeviceControl(DeviceControlModel deviceControlModel) {
        return deviceControlModelMapper.selectOne(deviceControlModel);
    }
}
