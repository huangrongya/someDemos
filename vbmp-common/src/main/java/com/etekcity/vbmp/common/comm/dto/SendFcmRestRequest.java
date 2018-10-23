/**
 *
 */
package com.etekcity.vbmp.common.comm.dto;

import java.util.List;

import com.etekcity.vbmp.common.config.VBMPRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author larry
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SendFcmRestRequest extends VBMPRequest {

    private String uuid;
    
    private List<String> sharedPeopleIds;
    
    private String msgKey;
    
    private String modelName;
}
