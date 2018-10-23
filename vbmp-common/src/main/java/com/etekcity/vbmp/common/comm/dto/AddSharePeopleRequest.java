package com.etekcity.vbmp.common.comm.dto;

import java.util.List;

import com.etekcity.vbmp.common.config.VBMPRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AddSharePeopleRequest extends VBMPRequest {

    private String uuid;

    private List<String> sharedPeoples;

}
