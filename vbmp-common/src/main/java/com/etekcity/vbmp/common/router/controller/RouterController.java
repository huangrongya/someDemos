package com.etekcity.vbmp.common.router.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.constant.ErrorConstant;
import com.etekcity.vbmp.common.router.dto.RequestBaseInfo;
import com.etekcity.vbmp.common.router.service.UserService;
import com.etekcity.vbmp.common.utils.MyStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName RouterController
 * @Description
 * @Author Ericer
 * @Date 09-17 下午1:35
 **/

@RestController
@RequestMapping("/platform/v1")
@Slf4j
public class RouterController {
    @Autowired
    private UserService userService;


    /**
     * 清除用户数据
     *
     * @param json
     * @return
     */
    @PostMapping("/user/deleteUserData")
    public VBMPResponse deleteUserData(@RequestBody String json) {
        RequestBaseInfo request = JSONObject.parseObject(json, RequestBaseInfo.class);
        log.info("请求删除用户数据参数:{}", JSONObject.toJSONString(request));
        VBMPResponse response = new VBMPResponse();
        // validate params
        if (MyStringUtils.isNullData(request.getAccountID(), request.getToken())) {
            response.setCode(ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            response.setMsg(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            log.error("请求参数不完整");
            return response;
        }
        // validate auth Todo 待7A完善后修改
        /*if (!tokenValidation.tokenValidation(request.getToken(), request.getAccountID())) {
            response.setCode(ErrorConstant.ERR_ACCOUNT_OR_PASSWORD_WRONG);
            response.setMsg(ErrorConstant.ERR_ACCOUNT_OR_PASSOWRD_WRONG_MSG);
            logger.info("权限验证失败");
            return response;
        }*/
        try {
            response = userService.deleteUserData(request);
            log.info("删除用户数据返回:{}", JSON.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.info("删除用户数据异常", e);
            response.setCode(ErrorConstant.ERR_DATABASE);
            response.setMsg(ErrorConstant.ERR_DATABASE_MSG);
            return response;
        }
    }

    /**
     * 拒绝协议
     *
     * @param jsonString
     * @return
     */
    @PostMapping("/user/rejectProtocol")
    public VBMPResponse rejectProtocol(@RequestBody String jsonString) {
        RequestBaseInfo request = JSONObject.parseObject(jsonString, RequestBaseInfo.class);
        log.info("拒绝协议请求：{}", JSONObject.toJSONString(request));
        VBMPResponse response = new VBMPResponse();
        // 参数验证
        if (MyStringUtils.isNullData(request.getAccountID(), request.getToken())) {
            response.setCode(ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            response.setMsg(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            log.error("拒绝协议请求参数不完整");
            return response;
        }
        // 验证权限
        /*if (!tokenValidation.tokenValidation(request.getToken(), request.getAccountID())) {
            response.setCode(ErrorConstant.ERR_ACCOUNT_OR_PASSWORD_WRONG);
            response.setMsg(ErrorConstant.ERR_ACCOUNT_OR_PASSOWRD_WRONG_MSG);
            logger.info("拒绝协议权限验证失败");
            return response;
        }*/
        try {
            userService.rejectProtocol(request.getAccountID());

            log.info("拒绝协议返回：{}", JSONObject.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.error("删除用户数据失败", e);
            response.setCode(ErrorConstant.ERR_DATABASE);
            response.setMsg(ErrorConstant.ERR_DATABASE_MSG);
            log.info("拒绝协议返回：{}", JSONObject.toJSONString(response));
            return response;
        }
    }


}
