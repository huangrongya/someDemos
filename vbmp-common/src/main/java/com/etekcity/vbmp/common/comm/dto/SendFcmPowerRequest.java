package com.etekcity.vbmp.common.comm.dto;

import java.util.List;

import com.etekcity.vbmp.common.config.VBMPRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SendFcmPowerRequest extends VBMPRequest {

    private String uuid;
    private List<String> sharePeopleIds;
    private String maxCost;
    private String threshold;
    private String modelName;
    private String msgKey;
}
