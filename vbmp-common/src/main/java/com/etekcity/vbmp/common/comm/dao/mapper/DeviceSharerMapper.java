package com.etekcity.vbmp.common.comm.dao.mapper;

import com.etekcity.vbmp.common.comm.dao.model.DeviceSharer;
import com.etekcity.vbmp.common.utils.MyMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeviceSharerMapper extends MyMapper<DeviceSharer> {

    int insertWithMyCat(DeviceSharer record);

    int insertSelectiveWithMyCat(DeviceSharer record);

//    int insertBatch(List<DeviceSharer> deviceSharers);

    int addExistSharers(@Param("deviceCid") String deviceCid, @Param("deviceSharerList") List<DeviceSharer> deviceSharers);

    /**
     * @Description: 通过userid和cid
     * @Author: royle.Huang
     * @Date: 2018/9/14
     */
    int updateByUserIdAndCid(DeviceSharer record);
}