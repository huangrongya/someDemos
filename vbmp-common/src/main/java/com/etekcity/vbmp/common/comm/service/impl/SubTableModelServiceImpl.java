package com.etekcity.vbmp.common.comm.service.impl;

import com.etekcity.vbmp.common.constant.CommonConstant;
import com.etekcity.vbmp.common.comm.dao.mapper.SubTableModelMapper;
import com.etekcity.vbmp.common.comm.dao.model.SubTableModel;
import com.etekcity.vbmp.common.comm.dao.model.SubTableQueryModel;
import com.etekcity.vbmp.common.comm.service.SubTableModelService;
import com.etekcity.vbmp.common.utils.MyStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * @ClassName SubTableModelServiceImpl
 * @Description
 * @Author Ericer
 * @Date 09-19 上午11:27
 **/
@Service
@Slf4j
public class SubTableModelServiceImpl implements SubTableModelService {
    @Autowired
    private SubTableModelMapper subTableModelMapper;

    /**
     * @return void
     * @Author Ericer
     * @Description 删除子表对应数据
     * @Date 下午2:52 18-9-19
     * @Param [cids]
     **/
    @Override
    public void deleteSubTableUserData(List<String> cids) {
        //查询所有子表
        List<SubTableModel> allSubTables = queryAll();
        if (allSubTables == null) {
            return;
        }
        //逐个删除子表
        for (SubTableModel subTableModel : allSubTables) {
            String subTableName = subTableModel.getSubTableName();
            SubTableQueryModel deleteSubTableModel = new SubTableQueryModel();
            deleteSubTableModel.setCids(MyStringUtils.covertListToStr(cids, CommonConstant.COMMA_STRING));
            deleteSubTableModel.setSubTableName(subTableName);
            deleteSubTableNameTable(deleteSubTableModel);
        }

    }

    @Override
    public void deleteSubTableUserData(String configModel, List<String> cids) {
        //查询所有子表
        List<SubTableModel> allSubTables = queryAll();
        if (allSubTables == null) {
            return;
        }
        //删除对应子表
        for (SubTableModel subTableModel : allSubTables) {
            List<String> configModelList = MyStringUtils.covertStrToList(subTableModel.getConfigModels(), CommonConstant.COMMA_STRING);
            if(configModelList.contains(configModel)) {
                String subTableName = subTableModel.getSubTableName();
                SubTableQueryModel deleteSubTableModel = new SubTableQueryModel();
                deleteSubTableModel.setCids(MyStringUtils.covertListToStr(cids, CommonConstant.COMMA_STRING));
                deleteSubTableModel.setSubTableName(subTableName);
                deleteSubTableModel.setConfigModel(configModel);
                deleteSubTableNameTable(deleteSubTableModel);
            }
        }
    }

    @Override
    public void deleteSubTableUserData(String configModel, String cid) {
        deleteSubTableUserData(configModel, Arrays.asList(new String[]{cid}));
    }

    @Override
    public void deleteSubTableUserDataByUuid(String configModel, String uuid) {
        //查询所有子表
        List<SubTableModel> allSubTables = queryAll();
        if (allSubTables == null) {
            return;
        }
        //删除对应子表
        for (SubTableModel subTableModel : allSubTables) {
            List<String> configModelList = MyStringUtils.covertStrToList(subTableModel.getConfigModels(), CommonConstant.COMMA_STRING);
            if(configModelList.contains(configModel)){
                String subTableName = subTableModel.getSubTableName();
                SubTableQueryModel deleteSubTableModel = new SubTableQueryModel();
                deleteSubTableModel.setUuids(uuid);
                deleteSubTableModel.setSubTableName(subTableName);
                deleteSubTableModel.setConfigModel(configModel);
                deleteSubTableNameTable(deleteSubTableModel);
                break;
            }
        }
    }

    @Override
    public List<SubTableModel> queryAll() {
        //Todo 添加redis缓存
        return subTableModelMapper.selectAll();
    }

    @Override
    public void deleteSubTableNameTable(SubTableQueryModel subTableModel) {
        subTableModelMapper.deleteSubTableNameTable(subTableModel);
    }


}
