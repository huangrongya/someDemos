/**
 *
 */
package com.etekcity.vbmp.common.comm.service.impl;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.comm.dto.UserInfoByUidRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoRequest;
import com.etekcity.vbmp.common.comm.dto.UserInfoResponse;
import com.etekcity.vbmp.common.comm.dto.UserLangInfoRequest;
import com.etekcity.vbmp.common.comm.dto.inner.UserInfo;
import com.etekcity.vbmp.common.comm.service.UserInfoService;
import com.etekcity.vbmp.common.config.VBMPResponse;

/**
 * @author puyol
 */
@Service
@Slf4j
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    CommonUserServiceImpl commonUserService;

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


	@Override
	public VBMPResponse syncUserDivice(UserInfoRequest request) {
		VBMPResponse response = new VBMPResponse();
		Integer syncDvice = commonUserService.syncUserDevices(request, request.getAccountIds());
		if(syncDvice != 0){
			response.setCode(-1);
			response.setMsg("同步设备列表错误");
		}
		return response;
	}
}
