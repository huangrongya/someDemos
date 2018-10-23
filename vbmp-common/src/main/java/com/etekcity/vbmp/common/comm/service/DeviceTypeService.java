package com.etekcity.vbmp.common.comm.service;

import java.util.List;

import com.etekcity.vbmp.common.config.VBMPRequest;
import com.etekcity.vbmp.common.comm.dao.model.DeviceType;
import com.etekcity.vbmp.common.comm.dto.SupportedModelsResponse;

public interface DeviceTypeService {

    void querySupportedmodels(SupportedModelsResponse supportedModelsResponse,  VBMPRequest requestBaseInfo);

    DeviceType getDeviceTypeByConfigModel(String configModel);

    DeviceType getDeviceType(String model);

    DeviceType getDeviceTypeByUuid(String uuid);

    List<DeviceType> findDeviceTypeByType(String type);

    List<DeviceType> queryTypes();
}
