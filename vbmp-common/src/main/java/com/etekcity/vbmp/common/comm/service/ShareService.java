package com.etekcity.vbmp.common.comm.service;

import com.etekcity.vbmp.common.comm.dto.*;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.comm.dao.model.DeviceSharer;

import java.util.List;

public interface ShareService {

    /**
     * @Description: 添加共享者
     * @Author: royle.Huang
     * @Date: 2018/9/13
     */
    VBMPResponse addSharePeople(AddSharePeopleRequest request);

    /**
     * @param request
     * @Description: 删除共享者
     * @Author: royle.Huang
     * @Date: 2018/9/13
     */
    VBMPResponse deleteSharePeople(DeleteSharePeopleRequest request);

    void deleteSharePeopleByUuidAndUserId(String userId, String uuid);

    /**
     * @Description: 查询共享者列表
     * @Author: royle.Huang
     * @Date: 2018/9/13
     */
    SharePeopleResponse querySharePeople(SharePeopleRequest request);

    /**
     * @Description: 查询历史共享者列表
     * @Author: royle.Huang
     * @Date: 2018/9/13
     * @param request
     */
    SharePeopleResponse querySharePeopleHistory(ShareHistoryRequest request);

    /**
     * @Description: 删除历史共享者
     * @Author: royle.Huang
     * @Date: 2018/9/13
     */
    VBMPResponse deleteSharePeopleHistory(SharePeopleRequest request);

    /**
     * @Description: 通过uuid和userId查询DeviceSharer
     * @Author: royle.Huang
     * @Date: 2018/9/13
     */
    DeviceSharer queryDeviceSharerByUuidAndUserId(String uuid, String userId);

    /**
     * @Description: 通过userId查询deviceShare列表
     * @Author: royle.Huang
     * @Date: 2018/9/13
     */
    List<DeviceSharer> queryDeviceShareListByUserId(String userId);

    /**
     * @Description: 通过uuid删除贡献者和历史共享者
     * @Author: royle.Huang
     * @Date: 2018/9/14
     */
    void deleteSharePeopleAllByUuid(String uuid);

    /**
     * @Description: 通过uuid查询分享者Id列表
     * @Author: royle
     * @Date: 2018/09/20
     */
    List<String> queryDeviceSharerListByUuid(String uuid);

    /**
     * @Description: 通过uuid删除所有共享信息
     * @Author: royle
     * @Date: 2018/09/25
     */
    VBMPResponse deleteShareDataByUuid(String uuid);
}
