package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.config.VBMPResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FirmwareStatusResponse extends VBMPResponse {

    private String currentFirmVersion;
    private String latestFirmVersion;
    /**
     * 0 升级成功 1 升级失败 2 升级启动失败 3 DNS解析失败 4 域名格式错误 5 正在升级中
     */
    private Integer updateSuccess;
}
