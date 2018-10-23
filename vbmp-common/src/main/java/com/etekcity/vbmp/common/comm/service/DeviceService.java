package com.etekcity.vbmp.common.comm.service;

import com.etekcity.vbmp.common.config.VBMPRequest;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.comm.dao.model.DeviceInfo;
import com.etekcity.vbmp.common.comm.dto.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DeviceService {

    GetDeviceDynamicInfoResponse getDeviceName(String deviceType, GetDeviceDynamicInfoRequest getDeviceDynamicInfoRequest);

    DeviceInfo queryDeviceByUuid(String uuid);

    VBMPResponse updateFirmware(String uuid, String accountId);

    VBMPResponse getFirmwareStatus(String accountId, String uuid);

    DevicesResponse getDevices(VBMPRequest request);

    DevicesGoogleHomeResponse getDevicesGoogleHome(VBMPRequest request);

    List<String> getUsedDeviceType(String accountId);

    List<DeviceInfo> getAccountDevice(String accountId);

    List<DeviceInfo> getOwnDevice(String accountId);

    List<DeviceInfo> getShareDevice(String accountId);

    DeviceInfo getDeviceByCid(String cid);

    int updateByPrimaryKey(DeviceInfo device);

    DeviceInfo getDeviceByCidAndAccountId(String cid, String account);

    @Transactional(rollbackFor = Exception.class)
    void deleteOwnSingleDevice(String accountId, String uuid, String deviceType);

    @Transactional(rollbackFor = Exception.class)
    VBMPResponse deleteVbmpOwnDevice(String uuid);

    @Transactional(rollbackFor = Exception.class)
    void deleteOwnSingleTableDevice(String accountId, String uuid, String configModel);

    void deleteOwnVdmpSingleDevice(DeviceInfo device);

    @Transactional(rollbackFor = Exception.class)
    VBMPResponse  deleteDevice(DeviceRequest deviceRequest) throws Exception;

    void deleteOwnDeviceRedis(String accountId, String uuid);

    void deleteShareDeviceInRedis(String accountId, String uuid);
}
