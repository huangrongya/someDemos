package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.config.VBMPRequest;
import lombok.Data;

@Data
public class DeviceRequest extends VBMPRequest {
    private String uuid;
}
