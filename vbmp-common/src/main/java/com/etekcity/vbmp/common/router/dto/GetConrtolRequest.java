package com.etekcity.vbmp.common.router.dto;

import lombok.Data;

import java.util.List;

@Data
public class GetConrtolRequest {

    /**
     * 设备uuid
     */
    private String uuid;

    /**
     * 协议识别号
     */
    private String identify;

    /**
     * 参数
     */
    private List<String> bodyParam;

    private String configModel;
}
