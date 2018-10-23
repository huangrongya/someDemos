package com.etekcity.vbmp.common.comm.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.comm.dao.model.DeviceInfo;
import com.etekcity.vbmp.common.config.VBMPRequest;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.constant.ErrorConstant;
import com.etekcity.vbmp.common.comm.dto.*;
import com.etekcity.vbmp.common.filter.aop.ApplicationResponse;
import com.etekcity.vbmp.common.filter.aop.Calibration;
import com.etekcity.vbmp.common.comm.service.DeviceService;
import com.etekcity.vbmp.common.comm.service.DeviceTypeService;
import com.etekcity.vbmp.common.utils.MyStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/vbmp/common/device")
@Slf4j
public class DeviceController {

    @Value("${token.remoteAddress}")
    private String tokenAddress;

    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceTypeService deviceTypeService;


    @PostMapping("/updatefirmware")
    @Calibration(checkToken = true)
    public VBMPResponse updateFirmware(@RequestBody String json) {
        DeviceFirmwareRequest request = JSON.parseObject(json, DeviceFirmwareRequest.class);
        log.info("请求更新固件接口参数:{}", JSON.toJSONString(request));
        VBMPResponse response = new VBMPResponse();
        // 验证uuid参数是否为空
        if (MyStringUtils.isNullData(request.getUuid())) {
            response.setCode(ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            response.setMsg(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            log.info("uuid参数校验为空");
            return response;
        }
        // 获取updatefirmwareReponse信息
        response = deviceService.updateFirmware(request.getUuid(), request.getAccountId());
        log.info("请求更新固件接口返回:{}", JSON.toJSON(response));
        return response;
    }

    /**
     * 获取固件版本号,验证升级是否成功
     *
     * @param json
     * @return
     */
    @PostMapping("/firmwarestatus")
    @Calibration(checkToken = true)
    public VBMPResponse firmwareStatus(@RequestBody String json) {
        DeviceFirmwareRequest request = JSON.parseObject(json, DeviceFirmwareRequest.class);
        log.info("调用获取固件版本或验证是否升级成功接口参数:{}", JSON.toJSONString(request));
        VBMPResponse response = new FirmwareStatusResponse();
        // 验证uuid参数是否为空
        if (MyStringUtils.isNullData(request.getUuid())) {
            response.setCode(ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            response.setMsg(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            log.info("uuid参数校验为空");
            return response;
        }
        // 获取energyYearResponse信息
        response = deviceService.getFirmwareStatus(request.getAccountId(), request.getUuid());
        log.info("调用获取固件版本或验证是否升级成功接口返回:{}", JSON.toJSON(response));
        return response;
    }

    /**
     * 获取设备列表
     *
     * @param json
     * @return
     */
    @PostMapping("/devices")
    @Calibration(checkToken = true)
    public VBMPResponse getDevices(@RequestBody String json) {
        VBMPRequest request = JSON.parseObject(json, VBMPRequest.class);
        log.info("获取设备列表接口参数:{}", JSON.toJSONString(request));
        DevicesResponse response = deviceService.getDevices(request);
        log.info("获取设备列表接口返回:{}", JSON.toJSON(response));
        return response;
    }


    /**
     * 获取设备动态信息（默认名称，用户语言等）
     *
     * @param json
     * @return
     */
    @PostMapping("/getDeviceDynamicInfo")
    @Calibration(fields = {"deviceType", "configModel"}, checkToken = true)
    public GetDeviceDynamicInfoResponse getDeviceDynamicInfo(@RequestBody String json) {
        GetDeviceDynamicInfoRequest request = JSON.parseObject(json, GetDeviceDynamicInfoRequest.class);
        String deviceType = request.getDeviceType();
        return deviceService.getDeviceName(deviceType, request);

    }

    /**
     * 获取支持的设备类型
     *
     * @param jsonString
     * @return
     */
    @PostMapping("/getSupportedmodels")
    @Calibration(checkToken = true)
    @ResponseBody
    public SupportedModelsResponse supportedmodels(@RequestBody String jsonString) {
        SupportedModelsRequest request = JSONObject.parseObject(jsonString, SupportedModelsRequest.class);

        SupportedModelsResponse response = new SupportedModelsResponse();
        deviceTypeService.querySupportedmodels(response, request);
        return response;

    }

    /**
     * 获取支持的设备类型
     *
     * @param jsonString
     * @return
     */
    @PostMapping("/deletedevice")
    @Calibration(fields = {"uuid"}, checkToken = true)
    @ApplicationResponse
    public VBMPResponse deleteDevice(@RequestBody String jsonString) throws Exception {
        DeviceRequest deviceRequest = JSONObject.parseObject(jsonString, DeviceRequest.class);
        VBMPResponse response = deviceService.deleteDevice(deviceRequest);
        return response;
    }

    @PostMapping("/getShareDevice")
    @Calibration(fields = {"accountId"})
    public GetShareDeviceResponse getShareDevice(@RequestBody String json) {
        VBMPRequest request = JSON.parseObject(json, VBMPRequest.class);
        List<DeviceInfo> deviceList = deviceService.getShareDevice(request.getAccountId());
        GetShareDeviceResponse response = new GetShareDeviceResponse();
        response.setDeviceInfoList(deviceList);
        return response;
    }

}
