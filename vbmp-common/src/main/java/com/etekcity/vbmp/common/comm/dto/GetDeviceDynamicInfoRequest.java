package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.config.VBMPRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetDeviceDynamicInfoRequest extends VBMPRequest {

    /**
     * 设备型号
     */
    private String deviceType;

    /**
     * 产品配置模式
     */
    private String configModel;
}
