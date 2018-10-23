package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.config.VBMPRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SendFcmShareRequest extends VBMPRequest {

    private String uuid;

    private String sharedPeopleId;

    private String msgKey;
    
    private String modelName;

}
