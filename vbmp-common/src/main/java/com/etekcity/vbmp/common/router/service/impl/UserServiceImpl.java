package com.etekcity.vbmp.common.router.service.impl;

import com.etekcity.vbmp.common.config.VBMPResponse;

import com.etekcity.vbmp.common.comm.dao.model.DeviceInfo;
import com.etekcity.vbmp.common.router.service.DeviceControlService;
import com.etekcity.vbmp.common.router.dto.RequestBaseInfo;
import com.etekcity.vbmp.common.router.service.UserService;
import com.etekcity.vbmp.common.comm.service.DeviceService;
import com.etekcity.vbmp.common.comm.service.ShareService;
import com.etekcity.vbmp.common.comm.service.SubTableModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName UserServiceImpl
 * @Description
 * @Author Ericer
 * @Date 09-17 下午2:12
 **/
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceControlService deviceControlService;
    @Autowired
    private SubTableModelService subTableModelService;
    @Autowired
    private ShareService shareService;

    @Override
    public VBMPResponse deleteUserData(RequestBaseInfo request) throws Exception {
        log.info("请求删除用户数据Service");
        VBMPResponse response = new VBMPResponse();
        String accountId = request.getAccountID();
        // 设备持有人
        List<DeviceInfo> ownDevices = deviceService.getOwnDevice(accountId);
        List<String> cids = new ArrayList<>();
        if (ownDevices != null && !ownDevices.isEmpty()) {
            for (DeviceInfo device : ownDevices) {
                deviceService.deleteOwnSingleDevice(device.getAccountId(), device.getUuid(), device.getDeviceType());

                cids.add(device.getDeviceCid());
            }
            // 发送消息删除平台数据
            deviceControlService.deleteVdmpUserData(ownDevices.get(0).getUuid(), accountId, cids);
        }
        // 分享人
        List<DeviceInfo> sharedDevices = deviceService.getShareDevice(accountId);
        if (sharedDevices != null && !sharedDevices.isEmpty()) {
            for (DeviceInfo device : sharedDevices) {
                //删除分享人
                shareService.deleteSharePeopleByUuidAndUserId(device.getUuid(), accountId);
            }
        }
        return response;
    }

    /**
     * 拒绝协议处理
     *
     * @param accountId
     * @return
     * @throws Exception
     */
    @Override
    public VBMPResponse rejectProtocol(String accountId) throws Exception {
        log.info("请求拒绝协议Service");
        VBMPResponse response = new VBMPResponse();
        // 设备持有人
        List<DeviceInfo> ownDevices = deviceService.getOwnDevice(accountId);
        if (ownDevices != null && !ownDevices.isEmpty()) {
            for (DeviceInfo device : ownDevices) {
                deviceService.deleteOwnVdmpSingleDevice(device);
            }
        }
        // 分享人
        List<DeviceInfo> sharedDevices = deviceService.getShareDevice(accountId);
        if (sharedDevices != null && !sharedDevices.isEmpty()) {
            for (DeviceInfo device : sharedDevices) {
                //删除分享人
                shareService.deleteSharePeopleByUuidAndUserId(device.getUuid(), accountId);
            }
        }
        return response;
    }


}
