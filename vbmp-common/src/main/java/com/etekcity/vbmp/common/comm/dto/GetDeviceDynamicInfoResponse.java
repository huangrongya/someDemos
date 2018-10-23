package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.config.VBMPResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetDeviceDynamicInfoResponse extends VBMPResponse {
    private String deviceName;

    public String getDeviceName() {
        return deviceName;
    }
}
