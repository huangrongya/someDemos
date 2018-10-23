/**
 *
 */
package com.etekcity.vbmp.common.comm.service;

import com.etekcity.vbmp.common.comm.dto.SendFcmRestRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoByUidRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoResponse;
import com.etekcity.vbmp.common.comm.dto.UserLangInfoRequest;
import com.etekcity.vbmp.common.config.VBMPResponse;

/**
 * @author puyol
 */
public interface UserInfoService {

    UserInfoResponse getUserInfoByUser(UserInfoRequest request);

    UserInfoResponse getUserInfoByUid(UserInfoByUidRequest request);

    UserInfoResponse getUserLangInfo(UserLangInfoRequest request);
    
    VBMPResponse syncUserDivice(UserInfoRequest request);
    
}
