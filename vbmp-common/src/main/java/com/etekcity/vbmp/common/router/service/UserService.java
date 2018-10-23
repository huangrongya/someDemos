package com.etekcity.vbmp.common.router.service;

import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.router.dto.RequestBaseInfo;

/**
 * @ClassName UserService
 * @Description
 * @Author Ericer
 * @Date 09-17 下午2:12
 **/
public interface UserService {

    VBMPResponse deleteUserData(RequestBaseInfo request) throws Exception;

    VBMPResponse rejectProtocol(String accountId) throws Exception;
}
