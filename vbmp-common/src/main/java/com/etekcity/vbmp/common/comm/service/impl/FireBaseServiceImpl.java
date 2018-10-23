package com.etekcity.vbmp.common.comm.service.impl;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.comm.dao.mapper.DeviceMapper;
import com.etekcity.vbmp.common.comm.dao.mapper.FireBaseMapper;
import com.etekcity.vbmp.common.comm.dao.model.DeviceInfo;
import com.etekcity.vbmp.common.comm.dao.model.FireBaseInfo;
import com.etekcity.vbmp.common.comm.dto.SendFcmPowerRequest;
import com.etekcity.vbmp.common.comm.dto.SendFcmRestRequest;
import com.etekcity.vbmp.common.comm.dto.SendFcmShareRequest;
import com.etekcity.vbmp.common.comm.service.DeviceService;
import com.etekcity.vbmp.common.comm.service.FireBaseService;
import com.etekcity.vbmp.common.config.VBMPRequest;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.constant.CommonConstant;
import com.etekcity.vbmp.common.constant.ErrorConstant;
import com.etekcity.vbmp.common.utils.MyBeanUtils;


@Service("fireBaseService")
@Slf4j
public class FireBaseServiceImpl implements FireBaseService {
    private static final Logger logger = LoggerFactory.getLogger(FireBaseService.class);

    public final static String AUTH_KEY_FCM = "AAAAy5kPnk0:APA91bF1x8SW9tshFgNEAN7wbXG855DwiX2kwQYUn7TLwowwDT9V79CFsXtIxaSTXlsrDvUOCNcefToZ5S5FcrfrxY7bbUMuGe_pe0EZRfAQDENb5qCfxQuEjPZVlmUsbfvBjUpEHxFW";// app服务密钥

    public final static String API_URL_FCM = "https://fcm.googleapis.com/fcm/send";// 谷歌推送地址

    /**
     * 用于filter相关鉴权使用
     **/
    private static final String FILTER_RESET_VALIDATE_TOKEN = "9525e7c3a3b78d7a74cc83b1c3c1071c";
    /**
     * 用于PowerExceed EnergyExceed鉴权使用
     **/
    private static final String POWER_ENERGY_VALIDATE_TOKEN = "9525e7c3a3b78d7a74cc83b1c3c1071c";

    @Autowired
    RestTemplate restTemplate;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    FireBaseMapper fireBaseMapper;
    @Value("${outlet7a.domin.url}")
    private String outlet7AAddress;

    private static String IOS = ":ios";
    private static String ANDROID = ":android";


    @Override
    public VBMPResponse sendSharedMsg(SendFcmShareRequest requestBaseInfo, String ownerNickName, String sharePeopleId, String uuid) {
        return sendShareOrUnshareCommonMsg(requestBaseInfo, ownerNickName, sharePeopleId, uuid);
    }

    @Override
    public VBMPResponse sendPowerExceedMsg(SendFcmPowerRequest request) {
        return sendPowerOrEnergyExceedCommonMsg(request);
    }

    /**
     * 发送设备重置消息
     *
     * @param humiDifier550DeviceTable
     */
    @Override
    public VBMPResponse sendDeviceResetMsg(SendFcmRestRequest request) {
        VBMPResponse respone = new VBMPResponse();
        try {
            DeviceInfo DeviceInfo = deviceService.queryDeviceByUuid(request.getUuid());
            if (null == DeviceInfo) {
                respone.setCode(ErrorConstant.ERR_DEVICE_NOT_EXIST);
                respone.setMsg(ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
                return respone;
            }
            Assert.notNull(DeviceInfo, "参数DeviceInfo不能为空");
            Assert.hasText(DeviceInfo.getDeviceName(), "参数Devicename不能为空");
            Assert.hasText(DeviceInfo.getDeviceCid(), "参数Devicecid不能为空");
            Assert.hasText(DeviceInfo.getDeviceImg(), "参数Deviceimg不能为空");
            Assert.hasText(DeviceInfo.getAccountId(), "参数Accountid不能为空");
            //是否通知为关,不发消息
            if (!CommonConstant.COMMON_INT_STATUS_ON.equals(DeviceInfo.getAllowNotify())) {
                respone.setMsg("不允许消息推送");
                return respone;
            }
            FireBaseInfo fir  = getFireBaseInfo(request.getModelName(),request.getMsgKey());
            
            //targetAccountIds
            List<String> sharePeopleIdList = request.getSharedPeopleIds();
            List<String> accountIdList = new ArrayList<>(sharePeopleIdList);
            accountIdList.add(DeviceInfo.getAccountId());
            String[] sharePeopleIDs = new String[accountIdList.size()];
            //获取要发送的消息类型
            String msgType = fir.getMsgValue();
            SendFcmRestRequest requestBaseInfoCopy = new SendFcmRestRequest();
            MyBeanUtils.copyPropertiesIgnoreCase(request, requestBaseInfoCopy);
            requestBaseInfoCopy.setToken(FILTER_RESET_VALIDATE_TOKEN);

            JSONObject jsonObject = sendFcmMsg(requestBaseInfoCopy, DeviceInfo, accountIdList.toArray(sharePeopleIDs), new String[]{DeviceInfo.getDeviceName()}, msgType);
            if (jsonObject == null) {
                respone.setCode(-1);
                respone.setMsg("sendDeviceReset调用7A接口无返回");
                return respone;
            }
            Integer code = jsonObject.getJSONObject("error").getInteger("code");
            log.info("发送消息失败，错误码：{}", code);
        } catch (Exception e) {
            respone.setCode(-1);
            respone.setMsg("sendDeviceReset发送消息失败");
            log.info("sendDeviceReset发送消息失败", e);
        }
        return respone;
    }


    /**
     * 分享或者被取消分享发送消息公共部分
     *
     * @param ownerPeopleId
     * @param sharePeopleId
     * @param uuid
     * @param isShared
     */
    /**
     * 分享或者被取消分享发送消息公共部分
     *
     * @param ownerPeopleId
     * @param sharePeopleId
     * @param uuid
     * @param isShared
     */
    private VBMPResponse sendShareOrUnshareCommonMsg(SendFcmShareRequest request, String ownerNickName, String sharePeopleId, String uuid) {
        VBMPResponse respone = new VBMPResponse();
        try {
            DeviceInfo DeviceInfo = deviceService.queryDeviceByUuid(uuid);
            if (null == DeviceInfo) {
                respone.setCode(ErrorConstant.ERR_DEVICE_NOT_EXIST);
                respone.setMsg(ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
                return respone;
            }
            Assert.hasLength(uuid, "sendShareOrUnshareCommonMsg uuid不能为空");
            Assert.hasLength(ownerNickName, "sendShareOrUnshareCommonMsg ownerNickName不能为空");
            Assert.hasLength(sharePeopleId, "sendShareOrUnshareCommonMsg sharePeopleId不能为空");
            Assert.notNull(request, "sendShareOrUnshareCommonMsg requestBaseInfo不能为空");
            //是否通知为关,不发消息
            if (!CommonConstant.COMMON_INT_STATUS_ON.equals(DeviceInfo.getAllowNotify())) {
                respone.setMsg("不允许消息推送");
                return respone;
            }
            
            FireBaseInfo fir  = getFireBaseInfo(request.getModelName(),request.getMsgKey());

            SendFcmShareRequest requestBaseInfoCopy = new SendFcmShareRequest();
            MyBeanUtils.copyPropertiesIgnoreCase(request, requestBaseInfoCopy);
            requestBaseInfoCopy.setToken(FILTER_RESET_VALIDATE_TOKEN);
            //获取要发送的消息类型
            List<String> messageVar = new ArrayList<>(3);
            messageVar.add(ownerNickName);
            messageVar.add(DeviceInfo.getDeviceName());
            String[] messageVars = new String[messageVar.size()];

            JSONObject jsonObject = sendFcmMsg(requestBaseInfoCopy, DeviceInfo, new String[]{sharePeopleId}, messageVar.toArray(messageVars), fir.getMsgValue());
            if (jsonObject == null) {
                respone.setCode(-1);
                respone.setMsg("sendShareOrUnshare调用7A接口无返回");
                return respone;
            }
            Integer code = jsonObject.getJSONObject("error").getInteger("code");
        } catch (Exception e) {
            respone.setCode(-1);
            respone.setMsg("sendShareOrUnshare发送消息失败");
            log.info("sendShareOrUnshare发送消息失败", e);
        }
        return respone;
    }

    /**
     * @param accountID
     * @param sharePeopleIdList
     * @param uuid
     * @param isEnergyExceed
     */
    private VBMPResponse sendPowerOrEnergyExceedCommonMsg(SendFcmPowerRequest request) {
        VBMPResponse respone = new VBMPResponse();
        try {
            Assert.hasLength(request.getUuid(), "sendPowerOrEnergyExceedCommonMsg uuid不能为空");
            Assert.hasLength(request.getAccountId(), "sendPowerOrEnergyExceedCommonMsg accountID不能为空");
            SendFcmPowerRequest requestBaseInfo = new SendFcmPowerRequest();
            requestBaseInfo.setAccountId(request.getAccountId());
            requestBaseInfo.setToken(POWER_ENERGY_VALIDATE_TOKEN);

            DeviceInfo DeviceInfo = deviceService.queryDeviceByUuid(request.getUuid());
            if (null == DeviceInfo) {
                respone.setCode(ErrorConstant.ERR_DEVICE_NOT_EXIST);
                respone.setMsg(ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG);
                return respone;
            }           //是否通知为关,不发消息
            if (!CommonConstant.COMMON_INT_STATUS_ON.equals(DeviceInfo.getAllowNotify())) {
                respone.setMsg("不允许消息推送");
                return respone;
            }
            
            FireBaseInfo fir  = getFireBaseInfo(request.getModelName(),request.getMsgKey());

            //获取要发送的消息类型
            List<String> messageVar = new ArrayList<>(3);
            String msgType;
            if (null == request.getThreshold() && request.getMaxCost() != null) {
                messageVar.add(request.getMaxCost());
                messageVar.add(DeviceInfo.getDeviceName());
            } else if (null != request.getThreshold() && request.getMaxCost() == null) {
                messageVar.add(request.getThreshold());
                messageVar.add(DeviceInfo.getDeviceName());
            }else {
            	respone.setCode(-1);
            	respone.setMsg("sendPower参数不能为空");
            	return respone;
            }
            String[] messageVars = new String[messageVar.size()];
            String[] sharePeopleIdS = new String[request.getSharePeopleIds().size()];

            JSONObject jsonObject = sendFcmMsg(requestBaseInfo, DeviceInfo, request.getSharePeopleIds().toArray(sharePeopleIdS), messageVar.toArray(messageVars), fir.getMsgValue());
            if (jsonObject == null) {
                respone.setCode(-1);
                respone.setMsg("sendPowerOrEnergyExceed调用7A接口无返回");
                return respone;
            }
            Integer code = jsonObject.getJSONObject("error").getInteger("code");
        } catch (Exception e) {
            respone.setCode(-1);
            respone.setMsg("sendPower发送消息失败");
            logger.info("sendPower发送消息失败", e);
        }
        return respone;
    }

    /**
     * 发送android\ios设备fcm通知
     *
     * @param androidToken
     * @param iosToken
     * @param message
     */
    private JSONObject sendFcmMsg(VBMPRequest requestBaseInfo, DeviceInfo DeviceTable, String[] targetAccountIds, String[] messageVar, String messageType) {
        Assert.notNull(DeviceTable, "sendFcmMsg humiDifier550DeviceTable 参数不能为空");
        Assert.hasLength(messageType, "sendFcmMsg messageType 参数不能为空");
        Assert.notEmpty(targetAccountIds, "sendFcmMsg targetAccountIds 参数不能为空");
        Assert.notEmpty(messageVar, "sendFcmMsg messageVar 参数不能为空");

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("accountID", requestBaseInfo.getAccountId());
        requestHeaders.add("tk", requestBaseInfo.getToken());
        if (StringUtils.hasLength(requestBaseInfo.getTimeZone())) {
            requestHeaders.add("tz", requestBaseInfo.getTimeZone());
        }

        JSONObject json = new JSONObject();
        json.put("deviceName", DeviceTable.getDeviceName());
        json.put("targetAccountIds", targetAccountIds);
        json.put("messageVar", messageVar);
        json.put("deviceImg", DeviceTable.getDeviceImg());
        json.put("cid", DeviceTable.getDeviceCid());
        json.put("messageType", messageType);

        HttpEntity<String> requestEntity = new HttpEntity<String>(json.toString(), requestHeaders);
        log.info(String.format("发送fcm到设备%s", json));
        String responseJsonStr = commonHttpsUTF8Request(outlet7AAddress.concat("/v1/thirdparty/device/sendFcmMsg"), HttpMethod.POST, requestEntity, String.class);
        log.info("fcm 返回值: {}", responseJsonStr);
        JSONObject jsonObject = JSONObject.parseObject(responseJsonStr);
        if (jsonObject != null) {
            log.info(jsonObject.toJSONString());
        }
        return jsonObject;
    }


    public String commonHttpsUTF8Request(String urlPath, HttpMethod httpMethod, HttpEntity<?> requestEntity, Class<String> responseType) {
        ResponseEntity<String> response = null;
        String responseString = null;
        try {
            //添加utf-8支持
            List<HttpMessageConverter<?>> converterList = restTemplate.getMessageConverters();
            HttpMessageConverter<?> converter = new StringHttpMessageConverter();
            ((StringHttpMessageConverter) converter).setDefaultCharset(Charset.forName("UTF-8"));
            if (!(converterList.get(0) instanceof StringHttpMessageConverter)) {
                converterList.add(0, converter);
                restTemplate.setMessageConverters(converterList);
            }

            logger.info("发送http请求url:{}, method:{}, request:{}", urlPath, httpMethod, JSONObject.toJSONString(requestEntity));
            response = restTemplate.exchange(urlPath, httpMethod, requestEntity, responseType);
            logger.info("接收http请求response:{}", JSONObject.toJSONString(response));
        } catch (HttpClientErrorException e) {
            logger.info("发送http请求错误", e);
            responseString = e.getResponseBodyAsString();
        } catch (Exception e) {
            logger.info("发送http请求错误", e);
        }
        //打印header错误码
        if (response != null && response.getHeaders().containsKey("statusCode")) {
            logger.warn("请求返回码：{}", JSONObject.toJSONString(response.getHeaders().get("statusCode")));
        }
        if (response != null) {
            responseString = response.getBody();
        }
        if (responseString == null) {
            responseString = "";
        }
        return responseString;
    }

    
    private FireBaseInfo getFireBaseInfo(String modelName,String msgKey){
        FireBaseInfo info = new FireBaseInfo();
        info.setModelName(modelName);
        info.setMsgKey(msgKey);
        FireBaseInfo fir =  fireBaseMapper.selectOne(info);
        if(null == fir){
        	return null;
        }
        return fir;
    }
}
