/**
 *
 */
package com.etekcity.vbmp.common.comm.service.impl;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.comm.dto.SendFcmPowerRequest;
import com.etekcity.vbmp.common.comm.dto.SendFcmRestRequest;
import com.etekcity.vbmp.common.comm.dto.SendFcmShareRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoByUidRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoResponse;
import com.etekcity.vbmp.common.comm.dto.UserLangInfoRequest;
import com.etekcity.vbmp.common.comm.dto.inner.UserInfo;
import com.etekcity.vbmp.common.comm.service.FireBaseService;
import com.etekcity.vbmp.common.comm.service.SendFcmUserService;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.constant.ErrorConstant;

/**
 * @author puyol
 */
@Service("sendFcmUserServiceImpl")
@Slf4j
public class SendFcmUserServiceImpl implements SendFcmUserService {

    @Autowired
    CommonUserServiceImpl commonUserService;
    @Autowired
    FireBaseService fireBaseService;


    @Override
    public VBMPResponse sendFcmUserAddOrDel(SendFcmShareRequest request) {
        VBMPResponse errorResponse = new VBMPResponse();
        //获取对应id值
        boolean isExist = commonUserService.checkAccountExist(request.getSharedPeopleId());
        if (!isExist) {
            log.info("sharePeopleId对应用户不存在！");
            errorResponse.setCode(ErrorConstant.ERR_INVALID_PARAM_FORMAT);
            errorResponse.setMsg(ErrorConstant.ERR_INVALID_PARAM_FORMAT_MSG);
            return errorResponse;
        }
        String nickName = commonUserService.getUserNickName(request.getAccountId());
        errorResponse = fireBaseService.sendSharedMsg(request, nickName, request.getSharedPeopleId(), request.getUuid());
        return errorResponse;
    }


    @Override
    public VBMPResponse sendFcmRest(SendFcmRestRequest request) {
        return fireBaseService.sendDeviceResetMsg(request);
    }


    @Override
    public VBMPResponse sendFcmPower(SendFcmPowerRequest request) {
        return fireBaseService.sendPowerExceedMsg(request);
    }


    @Override
    public UserInfoResponse getUserInfoByUser(UserInfoRequest request) {
        UserInfoResponse response = new UserInfoResponse();
        List<UserInfo> userlist = commonUserService.getUserInfoByHttp(request.getAccountId(),request.getAccountIds());
        response.setUserList(userlist);
        return response;
    }


    @Override
    public UserInfoResponse getUserInfoByUid(UserInfoByUidRequest request) {
        UserInfoResponse response = new UserInfoResponse();
        List<UserInfo> userlist = commonUserService.getAccountIdByAccount(request);
        response.setUserList(userlist);
        return response;
    }


    @Override
    public UserInfoResponse getUserLangInfo(UserLangInfoRequest request) {
        UserInfoResponse response = new UserInfoResponse();
        JSONObject obj = commonUserService.getUserLangInfo(request, request.getFields());
        if (null != obj) {
            response.setMsg(obj.toJSONString());
        }
        return response;
    }


}
