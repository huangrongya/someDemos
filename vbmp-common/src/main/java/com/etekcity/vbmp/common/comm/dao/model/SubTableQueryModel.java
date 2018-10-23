package com.etekcity.vbmp.common.comm.dao.model;

import lombok.Data;

/**
 * @ClassName SubTableModel
 * @Description
 * @Author Ericer
 * @Date 09-18 上午10:32
 **/
@Data
public class SubTableQueryModel {
    private Integer id;
    private String subTableName;
    /**
     * 子模块configModel
     */
    private String configModel;

    private String cids;

    private String uuids;
}
