package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.config.VBMPRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConfiginfoRequest extends VBMPRequest {

    private String region;

    private String authKey;

    private String pid;


}
