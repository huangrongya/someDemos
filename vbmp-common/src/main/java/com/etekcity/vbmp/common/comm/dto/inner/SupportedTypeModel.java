package com.etekcity.vbmp.common.comm.dto.inner;

import java.util.List;

import lombok.Data;

@Data
public class SupportedTypeModel {
    private String typeName;
    private String typeImg;
    private String type;
    private List<SupportedModel> supportedModels;
}
