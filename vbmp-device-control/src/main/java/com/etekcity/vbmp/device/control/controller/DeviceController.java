package com.etekcity.vbmp.device.control.controller;


import com.alibaba.fastjson.JSON;
import com.etekcity.vbmp.device.control.constant.ErrorConstant;
import com.etekcity.vbmp.device.control.dto.ErrorResponse;
import com.etekcity.vbmp.device.control.dto.GetConrtolRequest;
import com.etekcity.vbmp.device.control.dto.GetConrtolResponse;
import com.etekcity.vbmp.device.control.dto.InsertConrtolRequest;
import com.etekcity.vbmp.device.control.exception.VdmpException;
import com.etekcity.vbmp.device.control.service.DeviceGetConrtolService;
import com.etekcity.vbmp.device.control.utils.MyStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vbmp/control/device/")
public class DeviceController {

    private Logger logger = LoggerFactory.getLogger(DeviceController.class);


    @Autowired
    private DeviceGetConrtolService deviceGetConrtolService;

    @PutMapping("/getConrtol")
    public GetConrtolResponse getConrtol(@RequestBody String json) {
        GetConrtolRequest request = JSON.parseObject(json, GetConrtolRequest.class);
        logger.info("invoke getConrtol request:{}", JSON.toJSON(request));
        GetConrtolResponse response = new GetConrtolResponse();
        //验证参数是否存在
        if (MyStringUtils.isNullData(request.getUuid(), request.getIdentify(), request.getConfigModel()
        )) {
            response.setCode(ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            response.setMsg(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            logger.error("invoke getConrtol 请求参数不完整");
            return response;
        }
        /*if (request.getMsg() == null && request.getMsg().length <= 0) {
            response.setCode(ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            response.setMsg(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            logger.error("invoke getConrtol 请求参数不完整");
            return response;
        }*/

        try {
            response = deviceGetConrtolService.getConrtolService(request);
            logger.info("invoke getConrtol response:{}", JSON.toJSON(response));
            return response;
        } catch (NumberFormatException e) {
            //数字格式异常
            logger.error("invoke getConrtol error", e);
            response.setCode(ErrorConstant.ERR_REQUEST_INVALID_PARAM);
            response.setMsg(ErrorConstant.ERR_REQUEST_INVALID_PARAM_MSG);
            return response;
        } catch (VdmpException e) {
            //VDMP调取异常
            logger.error("invoke vdmp error", e);
            response.setCode(e.getCode());
            response.setMsg(e.getMsg());
            return response;
        } catch (Exception e) {
            logger.error("invoke getConrtol error", e);
            response.setCode(ErrorConstant.ERR_DATABASE);
            response.setMsg(ErrorConstant.ERR_DATABASE_MSG);
            return response;
        }

    }

    @PostMapping("/insertConrtol")
    public ErrorResponse insertConrtol(@RequestBody String json) {
        InsertConrtolRequest request = JSON.parseObject(json, InsertConrtolRequest.class);
        logger.info("invoke insertConrtol request:{}", JSON.toJSON(request));
        ErrorResponse response = new GetConrtolResponse();
        //验证参数是否存在
        if (MyStringUtils.isNullData(request.getAgreement(), request.getIdentify(), request.getUrl(), request.getHttpMethodId(), request.getParamMap()
        )) {
            response.setCode(ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            response.setMsg(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            logger.error("invoke insertConrtol 请求参数不完整");
            return response;
        }
        try {
            response = deviceGetConrtolService.insetConrtolService(request);
            logger.info("invoke insertConrtol response:{}", JSON.toJSON(response));
            return response;
        } catch (NumberFormatException e) {
            //数字格式异常
            logger.error("invoke insertConrtol error", e);
            response.setCode(ErrorConstant.ERR_REQUEST_INVALID_PARAM);
            response.setMsg(ErrorConstant.ERR_REQUEST_INVALID_PARAM_MSG);
            return response;
        } catch (VdmpException e) {
            //VDMP调取异常
            logger.error("invoke vdmp error", e);
            response.setCode(e.getCode());
            response.setMsg(e.getMsg());
            return response;
        } catch (Exception e) {
            logger.error("invoke insertConrtol error", e);
            response.setCode(ErrorConstant.ERR_DATABASE);
            response.setMsg(ErrorConstant.ERR_DATABASE_MSG);
            return response;
        }

    }

}
