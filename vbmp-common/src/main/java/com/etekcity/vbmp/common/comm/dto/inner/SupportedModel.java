package com.etekcity.vbmp.common.comm.dto.inner;

import lombok.Data;

@Data
public class SupportedModel {
    private String model;
    private String modelImg;
    private String modelName;
    private String connectionType;
    private String electricity;
    private String power;
    private String type;
    private String configModel;
    private String smartConfigVideoUrl;
    private String aPNConfigVideoUrl;
}
