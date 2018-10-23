/**
 *
 */
package com.etekcity.vbmp.common.comm.service;

import com.etekcity.vbmp.common.comm.dto.SendFcmPowerRequest;
import com.etekcity.vbmp.common.comm.dto.SendFcmRestRequest;
import com.etekcity.vbmp.common.comm.dto.SendFcmShareRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoByUidRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoResponse;
import com.etekcity.vbmp.common.comm.dto.UserLangInfoRequest;
import com.etekcity.vbmp.common.config.VBMPResponse;

/**
 * @author puyol
 */

public interface SendFcmUserService {

    /**
     * @Description: 删除共享着添加共享者发送消息
     * @Author: larry.yang
     * @Date: 2018/9/13
     */
    VBMPResponse sendFcmUserAddOrDel(SendFcmShareRequest request);

    VBMPResponse sendFcmRest(SendFcmRestRequest request);

    VBMPResponse sendFcmPower(SendFcmPowerRequest request);

    UserInfoResponse getUserInfoByUser(UserInfoRequest request);

    UserInfoResponse getUserInfoByUid(UserInfoByUidRequest request);

    UserInfoResponse getUserLangInfo(UserLangInfoRequest request);
    


}
