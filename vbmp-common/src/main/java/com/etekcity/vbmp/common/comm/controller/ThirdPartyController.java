package com.etekcity.vbmp.common.comm.controller;

import com.alibaba.fastjson.JSONArray;
import com.etekcity.vbmp.common.config.VBMPRequest;
import com.etekcity.vbmp.common.comm.dto.DevicesGoogleHomeResponse;
import com.etekcity.vbmp.common.router.service.RouterService;
import com.etekcity.vbmp.common.comm.service.DeviceService;
import com.etekcity.vbmp.common.comm.service.DeviceTypeService;
import com.etekcity.vbmp.common.comm.service.impl.CommonUserServiceImpl;
import com.etekcity.vbmp.common.utils.MyStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


@RestController
@RequestMapping("/platform/v1/thirdparty/")
@Slf4j
public class ThirdPartyController {

    @Value("${token.remoteAddress}")
    private String tokenAddress;

    @Autowired
    private DeviceService deviceService;
    @Autowired
    private RouterService routerService;
    @Autowired
    private CommonUserServiceImpl commonUserService;
    @Autowired
    private DeviceTypeService deviceTypeService;


    /**
     * googleHome/Alexa获取用户设备列表
     *
     * @param request
     * @param response
     */
    @GetMapping("/loadMain")
    public void loadMain(HttpServletRequest request, HttpServletResponse response) {
        try {
            String token = request.getHeader("tk");//获取所有的请求头
            if (!StringUtils.hasText(token)) {
                log.info("token 为空");
                commonResponse(response, "2", "devices", "[]");
                return;
            }
            log.info("loadMain获取用户设备列表".concat(token));
            // 解析token获取accountID
            String accountID = commonUserService.parseTokenToAccountId(token);
            if (!StringUtils.hasText(accountID)) {
                log.info("用户不存在");
                commonResponse(response, "2", "devices", "[]");
                return;
            }
            response.setHeader("userid", accountID);
            // 查询设备列表
            VBMPRequest devicesRequest = new VBMPRequest();
            devicesRequest.setToken(token);
            devicesRequest.setAccountId(accountID);
            DevicesGoogleHomeResponse googleHomeResponse = deviceService.getDevicesGoogleHome(devicesRequest);
            // 封装返回数据
            commonResponse(response, null, "devices", JSONArray.toJSONString(googleHomeResponse.getDeviceGoogleHomes()));
        } catch (Exception e) {
            log.warn("获得设备列表失败", e);
            commonResponse(response, "-1", "devices", "[]");
        }
    }

    /**
     * googleHome/Alexa控制设备开关操作
     *
     * @param httpServletRequest  action  on/off
     * @param httpServletResponse 设备操作是否成功  "11"：设备离线 "20": 设备无响应 "19":该用户没有此设备  成功不返回
     */
    @PostMapping("/devRequest")
    public void devRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        try {
            routerService.devRequest(httpServletRequest, httpServletResponse);
            return;
        } catch (Exception e) {
            log.warn("googleHome/Alexa控制设备开关操作", e);
        }
        commonResponse(httpServletResponse, "-1", "", "");
    }


    /**
     * 封装返回数据，成功不要传errorCode
     *
     * @param response
     * @param errorCode
     * @param title
     * @param value
     */
    private void commonResponse(HttpServletResponse response, String errorCode, String title, String value) {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE);
        //
        if (!MyStringUtils.isNullData(errorCode)) {
            response.setHeader("error", errorCode);
        } else if (!MyStringUtils.isNullData(title)) {
            // 封装数据到body
            try {
                PrintWriter out = response.getWriter();
                out.write("{\"".concat(title).concat("\":").concat(value).concat("}"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
