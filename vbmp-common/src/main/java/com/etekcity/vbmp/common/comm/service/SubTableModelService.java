package com.etekcity.vbmp.common.comm.service;

import com.etekcity.vbmp.common.comm.dao.model.SubTableModel;
import com.etekcity.vbmp.common.comm.dao.model.SubTableQueryModel;

import java.util.List;

/**
 * @ClassName SubTableModelService
 * @Description
 * @Author Ericer
 * @Date 09-19 上午11:27
 **/
public interface SubTableModelService {
    void deleteSubTableUserData(List<String> cids);

    void deleteSubTableUserData(String configModel, List<String> cids);

    void deleteSubTableUserData(String configModel, String cid);

    void deleteSubTableUserDataByUuid(String configModel, String uuid);

    List<SubTableModel> queryAll();

    void deleteSubTableNameTable(SubTableQueryModel subTableModel);
}
