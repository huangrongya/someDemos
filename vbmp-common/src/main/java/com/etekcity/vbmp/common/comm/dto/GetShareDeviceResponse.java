package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.comm.dao.model.DeviceInfo;
import com.etekcity.vbmp.common.config.VBMPResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetShareDeviceResponse extends VBMPResponse {

    List<DeviceInfo> deviceInfoList;
}
