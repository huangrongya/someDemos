package com.etekcity.vbmp.common.comm.dao.mapper;

import com.etekcity.vbmp.common.comm.dao.model.SubTableModel;
import com.etekcity.vbmp.common.comm.dao.model.SubTableQueryModel;
import com.etekcity.vbmp.common.utils.MyMapper;

public interface SubTableModelMapper extends MyMapper<SubTableModel> {

    int deleteSubTableNameTable(SubTableQueryModel subTableQueryModel);
}
