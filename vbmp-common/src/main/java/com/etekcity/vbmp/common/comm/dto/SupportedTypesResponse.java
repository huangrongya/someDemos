package com.etekcity.vbmp.common.comm.dto;

import java.util.List;

import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.comm.dto.inner.SupportedType;

import lombok.Data;

@Data
public class SupportedTypesResponse extends VBMPResponse {

    private List<SupportedType> supportedTypes;

}
