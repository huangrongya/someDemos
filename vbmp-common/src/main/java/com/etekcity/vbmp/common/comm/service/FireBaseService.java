package com.etekcity.vbmp.common.comm.service;

import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.comm.dto.SendFcmPowerRequest;
import com.etekcity.vbmp.common.comm.dto.SendFcmRestRequest;
import com.etekcity.vbmp.common.comm.dto.SendFcmShareRequest;

public interface FireBaseService {

    VBMPResponse sendSharedMsg(SendFcmShareRequest requestBaseInfo, String ownerNickName, String sharePeopleId, String uuid);

    VBMPResponse sendPowerExceedMsg(SendFcmPowerRequest request);

    //VBMPResponse sendEnergyExceedMsg(String ownerPeopleId, List<String> sharePeopleIdList, String uuid);

    VBMPResponse sendDeviceResetMsg(SendFcmRestRequest requestBaseInfo);


}
