package com.etekcity.vbmp.common.router.controller;

import com.alibaba.fastjson.JSON;
import com.etekcity.vbmp.common.comm.service.DeviceService;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.constant.ErrorConstant;
import com.etekcity.vbmp.common.utils.MyStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName DeviceInnerController
 * @Description
 * @Author Ericer
 * @Date 09-27 下午5:34
 **/
@RestController
@RequestMapping("/vbmp/common/device")
@Slf4j
public class DeviceInnerController {

    @Autowired
    private DeviceService deviceService;

    /**
     * @Author Ericer
     * @Description 删除设备
     * @Date 下午5:32 18-9-27
     * @Param [uuid]
     * @return com.etekcity.vbmp.common.config.VBMPResponse
     **/
    @DeleteMapping("deletedevice")
    public VBMPResponse deleteDeviceByUuid(@RequestBody String uuid){
        VBMPResponse response = new VBMPResponse();
        log.info("请求删除设备（设备重置）:{}", uuid);
        // validate params
        if (MyStringUtils.isNullData(uuid)){
            response.setCode(ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            response.setMsg(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            log.error("请求参数不完整");
            return response;
        }
        try {
            response = deviceService.deleteVbmpOwnDevice(uuid);
            log.info("删除设备数据返回:{}", JSON.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.info("删除设备数据异常", e);
            response.setCode(ErrorConstant.ERR_DATABASE);
            response.setMsg(ErrorConstant.ERR_DATABASE_MSG);
            return response;
        }
    }


}
