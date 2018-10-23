package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.config.VBMPResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetConfiginfoResponse extends VBMPResponse {

    private String configkey;

    private String serverUrl;

    private String ip;

    private String pid;
}
