package com.etekcity.vbmp.common.router.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.constant.CommonConstant;
import com.etekcity.vbmp.common.constant.ConfigModelEnum;
import com.etekcity.vbmp.common.comm.dao.model.DeviceInfo;
import com.etekcity.vbmp.common.comm.dto.DeviceRequest;
import com.etekcity.vbmp.common.router.service.DeviceControlService;
import com.etekcity.vbmp.common.router.dto.GetConrtolRequest;
import com.etekcity.vbmp.common.router.dto.RequestBaseInfo;
import com.etekcity.vbmp.common.router.service.RouterService;
import com.etekcity.vbmp.common.router.service.VbmpRouterService;
import com.etekcity.vbmp.common.comm.service.DeviceControlModelService;
import com.etekcity.vbmp.common.comm.service.DeviceService;
import com.etekcity.vbmp.common.comm.service.impl.CommonUserServiceImpl;
import com.etekcity.vbmp.common.comm.service.impl.HttpClientService;
import com.etekcity.vbmp.common.utils.MyStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @ClassName RouterServiceImpl
 * @Description
 * @Author Ericer
 * @Date 09-14 上午11:04
 **/
@Service
@Slf4j
public class RouterServiceImpl implements RouterService {

    @Autowired
    private HttpClientService httpClientService;
    @Autowired
    private VbmpRouterService vbmpRouterService;
    @Autowired
    private DeviceControlService deviceControlService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private CommonUserServiceImpl commonUserService;
    @Autowired
    private DeviceControlModelService deviceControlModelService;

    @Value("${VBMP.device.controlModel.lightNight}")
    private String lightNightSuffix;


    @Override
    public VBMPResponse deleteDevice(DeviceRequest deviceRequest) {
        VBMPResponse response = new VBMPResponse();

        return response;
    }


    /**
     * @param httpServletRequest
     * @param httpServletResponse
     */
    @Override
    public void devRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        //获取所有的请求头
        String token = httpServletRequest.getHeader("tk");
        log.info("devRequest控制设备开关操作".concat(token));
        if (!StringUtils.hasText(token)) {
            log.warn("token 为空");
            commonResponse(httpServletResponse, "2", "msg", "token");
            return;
        }
        //解析token获取accountID
        String accountID = commonUserService.parseTokenToAccountId(token);

        String bodyMsg = MyStringUtils.readRequestBody(httpServletRequest);
        JSONObject jsonObject = JSONObject.parseObject(bodyMsg);
        //封装参数到JSONObject，原样返回
        jsonObject.put("accountID", accountID);
        jsonObject.put("token", token);

        //验证必要参数
        //获取cid空
        String cid = jsonObject.getString("cid");
        if (!StringUtils.hasText(cid)) {
            log.warn("cid参数错误{}", cid);
            commonResponse(httpServletResponse, "-1", "msg", "cid参数错误");
            return;
        }

        String action = jsonObject.getString("action");
        if (!StringUtils.hasText(action)) {
            log.warn("action参数错误");
            commonResponse(httpServletResponse, "-1", "msg", "action参数错误");
            return;
        }

        //解析参数拆分为cid和对应设备类型configModel
        String[] cidArray = cid.split(CommonConstant.SEMICOLON_STRING);
        if (cidArray == null || cidArray.length == 0) {
            log.warn("cid参数解析错误{}", cid);
            commonResponse(httpServletResponse, "-1", "", "");
            return;
        }
        //替换cid
        jsonObject.put("cid", cidArray[0]);

        //获取configModel，根据configModel调用对应的模块功能
        String configModel;
        if (cidArray.length == 1) {
            configModel = ConfigModelEnum.Outlet7A.getConfigModel();
        } else {
            configModel = cidArray[1];
        }

        jsonObject.put("configModel", configModel);

        //控制设备
        if (ConfigModelEnum.Outlet7A.getConfigModel().equals(configModel)) {
            changeStatusOutlet7A(jsonObject, httpServletResponse);
        } else {
            DeviceInfo deviceInfo = deviceService.getDeviceByCidAndAccountId(cidArray[0], accountID);
            //用户不拥有此设备
            if (deviceInfo == null) {
                commonResponse(httpServletResponse, "19", "", "");
                return;
            }

            jsonObject.put("uuid", deviceInfo.getUuid());
            //不成功返回错误码
            Integer code = changeCommonDeviceStatus(jsonObject);
            if (code != 0) {
                log.info("设备控制错误:".concat(String.valueOf(code)));
                commonResponse(httpServletResponse, String.valueOf(code), "", "");
                return;
            } else {
                commonResponse(httpServletResponse, "", "", "");
                return;
            }
        }
    }


    /**
     * 7A设备操作
     *
     * @param jsonObject
     * @param httpServletResponse
     */
    @Override
    public void changeStatusOutlet7A(JSONObject jsonObject, HttpServletResponse httpServletResponse) {
        //获取cid
        String cid = jsonObject.getString("cid");
        //open break (设备开 关)
        String action = jsonObject.getString("action");
        //relay 命令类型 固定不变
        String uri = jsonObject.getString("uri");
        if (!CommonConstant.COMMON_STATUS_ON.equals(action) && !CommonConstant.COMMON_STATUS_OFF.equals(action)) {
            log.info("action参数错误");
            commonResponse(httpServletResponse, "-1", "", "");
            return;
        }


        //7A设备
        RequestBaseInfo devicesRequest = new RequestBaseInfo();
        devicesRequest.setAccountID(jsonObject.getString("accountID"));
        devicesRequest.setToken(jsonObject.getString("token"));
        Integer code = deviceControlService.change7aOutletStatus(devicesRequest, cid, action);
        if (code == 0) {
            commonResponse(httpServletResponse, "", "", "");
        } else {
            commonResponse(httpServletResponse, String.valueOf(code), "", "");
        }
    }


    private final String relay = "relay";
    private final String level = "level";
    private final String mode = "mode";
    private final String mode_manual = "manual";

    /**
     * 其他控制设备（通用）
     *
     * @param requestJsonObject
     * @return
     */
    @Override
    public Integer changeCommonDeviceStatus(JSONObject requestJsonObject) {
        String cid = requestJsonObject.getString("cid");
        String uuid = requestJsonObject.getString("uuid");
        String action = requestJsonObject.getString("action");
        String operation = requestJsonObject.getString("uri");
        String configModel = requestJsonObject.getString("configModel");

        //查询对应指令identify
        String identify = deviceControlModelService.getVdmpConrtolIdentifyByModel(configModel, operation);

        GetConrtolRequest vbmpControlRequest = new GetConrtolRequest();
        vbmpControlRequest.setUuid(uuid);
        vbmpControlRequest.setConfigModel(configModel);
        vbmpControlRequest.setIdentify(identify);

        Integer code = -1;
        List<String> bodyParam = new ArrayList<>();
        if (cid.indexOf(CommonConstant.GOOGLE_HOME_CID_NIGHT_LIGHTT) >= 0) {
            if (CommonConstant.COMMON_STATUS_OFF.equals(action)) {
                bodyParam.add("manual");
                bodyParam.add(action);
            } else {
                bodyParam.add("auto");
                bodyParam.add("");
            }
            vbmpControlRequest.setBodyParam(bodyParam);
            vbmpControlRequest.setIdentify(identify.concat(lightNightSuffix));
            code = deviceControlService.changeCommonDeviceStatus(vbmpControlRequest);
        } else if (operation.equals(relay)) {
            bodyParam.add(action);
            vbmpControlRequest.setBodyParam(bodyParam);
            code = deviceControlService.changeCommonDeviceStatus(vbmpControlRequest);
        } else if (operation.equals(level)) {
            bodyParam.add("manual");
            bodyParam.add(action);
            vbmpControlRequest.setBodyParam(bodyParam);
            code = deviceControlService.changeCommonDeviceStatus(vbmpControlRequest);
        } else if (operation.equals(mode)) {
            bodyParam.add(action);
            if (mode_manual.equals(action)) {
                //手动模式默认1级
                bodyParam.add(String.valueOf(1));
            }else{
                bodyParam.add("");
            }
            vbmpControlRequest.setBodyParam(bodyParam);
            code = deviceControlService.changeCommonDeviceStatus(vbmpControlRequest);
        } else {
            log.error("不支持的指令{}", operation);
        }
        return code;
    }

    /**
     * 封装返回数据，成功不要传errorCode
     *
     * @param httpServletResponse
     * @param errorCode
     * @param title
     * @param value
     */
    private void commonResponse(HttpServletResponse httpServletResponse, String errorCode, String title, String value) {
        //设置将字符以"UTF-8"编码输出到客户端浏览器
        httpServletResponse.setCharacterEncoding("UTF-8");
        //通过设置响应头控制浏览器以UTF-8的编码显示数据
        httpServletResponse.setHeader("content-type", "application/json;charset=UTF-8");
        //
        if (StringUtils.hasText(errorCode)) {
            httpServletResponse.setHeader("error", errorCode);
        } else if (StringUtils.hasText(title)) {
            //封装数据到body
            try {
                PrintWriter out = httpServletResponse.getWriter();
                out.write("{\"".concat(title).concat("\":").concat(value).concat("}"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
