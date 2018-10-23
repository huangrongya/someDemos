package com.etekcity.vbmp.common.comm.controller;

import java.util.Date;

import javax.annotation.Resource;

import com.etekcity.vbmp.common.comm.dao.model.DeviceType;
import com.etekcity.vbmp.common.comm.service.DeviceTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.config.redis.RedisServiceImpl;
import com.etekcity.vbmp.common.constant.CommonConstant;
import com.etekcity.vbmp.common.constant.ErrorConstant;
import com.etekcity.vbmp.common.filter.aop.Calibration;
import com.etekcity.vbmp.common.comm.dto.ConfiginfoRequest;
import com.etekcity.vbmp.common.comm.dto.GetConfiginfoRequest;
import com.etekcity.vbmp.common.comm.dto.GetConfiginfoResponse;
import com.etekcity.vbmp.common.comm.dto.GetConnectStatusRequest;
import com.etekcity.vbmp.common.comm.dto.GetConnectStatusResponse;
import com.etekcity.vbmp.common.utils.MyStringUtils;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/vbmp/common/net")
@Slf4j
public class DistributionNetworkController {

    private static final Logger logger = LoggerFactory.getLogger(DistributionNetworkController.class);

    @Value("${VDMP.Address}")
    private String address;
    @Value("${openapi.authkey}")
    private String authKey;
    @Value("${openapi.accessid}")
    private String accessId;
    @Value("${openapi.accesskey}")
    private String accessKey;

    @Autowired
    RestTemplate restTemplate;
    @Autowired
    private DeviceTypeService deviceTypeService;
    @Resource
    RedisServiceImpl redisServiceImpl;

    @PostMapping("/configinfo")
    @Calibration(fields = {"region", "configModel"}, checkToken = true)
    public VBMPResponse getConfiginfo(@RequestBody String json) {
        GetConfiginfoRequest request = JSON.parseObject(json, GetConfiginfoRequest.class);
        GetConfiginfoResponse getConfiginfoResponse = new GetConfiginfoResponse();
        try {
            String pid = null;
            DeviceType deviceType = deviceTypeService.getDeviceTypeByConfigModel(request.getConfigModel());
            if (deviceType != null) {
                pid = deviceType.getPid();
            }
            if (!StringUtils.hasText(pid)) {
                getConfiginfoResponse.setCode(ErrorConstant.ERR_DATABASE);
                getConfiginfoResponse.setMsg(ErrorConstant.ERR_DATABASE_MSG);
                logger.info("未查询到ConfigModel对应的pid");
                return getConfiginfoResponse;
            }
            logger.info("请求参数为：accessId={},accessKey={},authKey={},pid={}", accessId, accessKey, authKey, pid);

            HttpHeaders requestHeaders = new HttpHeaders();
            Date date = new Date();
            long timestamp = date.getTime();
            String params = "accessID=" + accessId + "&timestamp=" + timestamp + "&accessKey=" + accessKey;
            String md5str = org.springframework.util.DigestUtils.md5DigestAsHex(params.getBytes());
            String path = "accessID=" + accessId + "&timestamp=" + timestamp + "&sign=" + md5str.toUpperCase() + "";

            ConfiginfoRequest configinfoRequest = new ConfiginfoRequest();
            configinfoRequest.setRegion(request.getRegion());
            configinfoRequest.setAuthKey(authKey);
            configinfoRequest.setPid(pid);

            String jString = JSONObject.toJSONString(configinfoRequest);
            logger.info("invoke path is:" + address + "/connectCenter/v1/device/Configinfo?" + path);
            HttpEntity<String> requestEntity = new HttpEntity<>(jString, requestHeaders);
            ResponseEntity<String> response = restTemplate.exchange(address + "/connectCenter/v1/device/Configinfo?" + path,
                    HttpMethod.POST, requestEntity, String.class);
            JSONObject jsonObjresponse = JSONObject.parseObject(response.getBody());
            logger.info("invoke VDMP platform Configinfo result:" + jsonObjresponse);
            if (!"0".equals(jsonObjresponse.getString("code"))) {
                getConfiginfoResponse.setCode(Integer.parseInt(jsonObjresponse.getString("code")));
                getConfiginfoResponse.setMsg(jsonObjresponse.getString("msg"));
                return getConfiginfoResponse;
            }
            JSONObject data = jsonObjresponse.getJSONObject("data");
            getConfiginfoResponse.setConfigkey(data.getString("configkey"));
            getConfiginfoResponse.setIp(data.getString("ip"));
            getConfiginfoResponse.setServerUrl(data.getString("serverUrl"));
            getConfiginfoResponse.setPid(pid);
            logger.info("获取配网信息结束");

            return getConfiginfoResponse;
        } catch (RestClientException e) {
            logger.info("获取配网信息异常", e);
            getConfiginfoResponse.setCode(ErrorConstant.ERR_INTERNAL_SERVER);
            getConfiginfoResponse.setMsg(ErrorConstant.ERR_INTERNAL_SERVER_MSG);
            return getConfiginfoResponse;
        } catch (NumberFormatException e) {
            logger.info("获取配网信息异常", e);
            getConfiginfoResponse.setCode(ErrorConstant.ERR_INTERNAL_SERVER);
            getConfiginfoResponse.setMsg(ErrorConstant.ERR_INTERNAL_SERVER_MSG);
            return getConfiginfoResponse;
        }
    }

    /**
     * 获取设备注册（配网）状态
     *
     * @param json
     * @return
     */
    @PostMapping("/connectStatus")
    @Calibration(fields = {"configKey"}, checkToken = true)
    public GetConnectStatusResponse getConnectStatus(@RequestBody String json) {
        GetConnectStatusRequest request = JSON.parseObject(json, GetConnectStatusRequest.class);
        logger.info("invoke connectStatus request:" + JSON.toJSONString(request));
        GetConnectStatusResponse getConnectStatusResponse = new GetConnectStatusResponse();
        try {
            Date date = new Date();
            long timestamp = date.getTime();
            String params = "accessID=" + accessId + "&timestamp=" + timestamp + "&accessKey=" + accessKey;
            String md5str = org.springframework.util.DigestUtils.md5DigestAsHex(params.getBytes());
            String path = "accessID=" + accessId + "&timestamp=" + timestamp + "&sign=" + md5str.toUpperCase() + "";

            ResponseEntity<String> response = restTemplate.exchange(address + "/connectCenter/v1/device/config/"
                    + request.getConfigKey() + "/connectStatus?" + path, HttpMethod.GET, null, String.class);

            JSONObject jsonObjresponse = JSONObject.parseObject(response.getBody());
            logger.info("invoke VDMP connectStatus result:" + jsonObjresponse);
            String errcode = jsonObjresponse.getString("code");
            if (!"0".equals(errcode)) {
                getConnectStatusResponse.setCode(Integer.valueOf(errcode));
                getConnectStatusResponse.setMsg(jsonObjresponse.getString("msg"));
                return getConnectStatusResponse;
            }

            JSONObject data = jsonObjresponse.getJSONObject("data");
            String UUID = data.getString("uuid");
            String cid = data.getString("cid");
            //uuid cid判断        //TODO
            if (!"ok".equalsIgnoreCase(data.getString("state"))) {
                getConnectStatusResponse.setCode(ErrorConstant.ERR_INVALID_CID);
                getConnectStatusResponse.setMsg(data.getString("state"));
                return getConnectStatusResponse;
            }

            if (MyStringUtils.isNullData(UUID, cid)) {
                getConnectStatusResponse.setCode(ErrorConstant.ERR_INVALID_CID);
                getConnectStatusResponse.setMsg(ErrorConstant.ERR_INVALID_CID_MSG);
                return getConnectStatusResponse;
            }
            //保存uuid和cid关系
            redisServiceImpl.addMap(CommonConstant.REDIS_KEY_PLANTE_ACCONUTID_PREFIX.concat(request.getAccountId()), "uuid-".concat(UUID), cid, CommonConstant.SECONDS_OF_ONEDAY);
            getConnectStatusResponse.setState(data.getString("state"));
            getConnectStatusResponse.setUuid(UUID);
            logger.info("获取设备注册状态结束{}", JSONObject.toJSONString(getConnectStatusResponse));
            return getConnectStatusResponse;
        } catch (Exception e) {
            logger.info("获取设备注册状态异常", e);
            getConnectStatusResponse.setCode(ErrorConstant.ERR_INTERNAL_SERVER);
            getConnectStatusResponse.setMsg(ErrorConstant.ERR_INTERNAL_SERVER_MSG);
            return getConnectStatusResponse;
        }

    }
}
