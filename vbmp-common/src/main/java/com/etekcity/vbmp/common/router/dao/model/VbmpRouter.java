package com.etekcity.vbmp.common.router.dao.model;

import lombok.Data;

/**
 * @ClassName VbmpRouter
 * @Description
 * @Author Ericer
 * @Date 下午6:01
 **/
@Data
public class VbmpRouter {
    private Integer id;
    private String configModel;
    private String uriPrefix;
    private String domin;
    private String createTime;
}
