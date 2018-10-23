package com.etekcity.vbmp.common.router.dao.model;

import lombok.Data;

/**
 * @ClassName VbmpRouter
 * @Description
 * @Author Ericer
 * @Date 下午6:01
 **/
@Data
public class DeviceControlModel {
    private Integer id;
    private String configModel;
    private String identify;
    private String operation;
}
