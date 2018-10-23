package com.etekcity.vbmp.common.comm.service.impl;


import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.comm.dto.RequestBaseInfo;
import com.etekcity.vbmp.common.comm.dto.UserInfoByUidRequest;
import com.etekcity.vbmp.common.comm.dto.inner.UserInfo;
import com.etekcity.vbmp.common.config.VBMPRequest;
import com.etekcity.vbmp.common.exception.MyResponseErrorHandler;
import com.etekcity.vbmp.common.exception.ServiceException;

/**
 *
 */
@Service("commonUserService")
@Slf4j
public class CommonUserServiceImpl {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    RestTemplate restTemplate;
    @Value("${outlet7a.domin.url}")
    private String outlet7AAddress;
    @Value("${token.remoteAddress}")
    private String tokenAddress;
    @Autowired
    private HttpClientService httpClientService;

    private static final String USER_TOKEN = "C29E7D0C0F62C104A97442BBE2909E52";

    /**
     * 将token转换为accountid
     *
     * @param token
     * @return
     */
    public String parseTokenToAccountId(String token) {
        String url = tokenAddress.concat("/parseTK");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);
        JSONObject json = new JSONObject();
        json.put("tk", token);
        HttpEntity<String> httpEntity = new HttpEntity<>(json.toJSONString(), headers);
        String jsonStr = httpClientService.commonRequest(url, HttpMethod.POST, httpEntity, String.class);
        JSONObject jsonObject = JSON.parseObject(jsonStr);
        String accountId = jsonObject.getString("accountid");
        if (accountId == null) {
            log.info("token转换为accountid失败：".concat(jsonStr));
        }
        return accountId;
    }

    /**
     * 获得用户account
     *
     * @param accountId
     * @return
     */
    public String getUserAccount(String accountId) {
        Assert.hasText(accountId, "getUserAccount方法accountId参数不能为空");
        List<UserInfo> userInfoList = getSingleUserInfoByHttp(accountId);
        String account = null;
        if (!userInfoList.isEmpty()) {
            account = userInfoList.get(0).getAccount();
        }
        return account;
    }

    /**
     * 获得用户昵称
     *
     * @param accountID
     * @return
     */
    public String getUserNickName(String accountID) {
        String nickName = "";
        List<UserInfo> userInfoList = getSingleUserInfoByHttp(accountID);
        if (!userInfoList.isEmpty()) {
            nickName = userInfoList.get(0).getNickName();
        }
        return nickName;
    }

    /**
     * 获得用户语言
     *
     * @param accountId
     * @return
     */
    public String getAccepyLanguage(String accountId) {
        String acceptLanguage = "en";
        List<UserInfo> userInfoList = getSingleUserInfoByHttp(accountId);
        if (!userInfoList.isEmpty()) {
            acceptLanguage = userInfoList.get(0).getAcceptLanguage();
        }
        return acceptLanguage;
    }


    /**
     * 判断用户是否存在
     *
     * @param accountID
     * @return
     */
    public boolean checkAccountExist(String accountID) {
        List<UserInfo> userInfoList = getUserInfoByHttp(accountID,Arrays.asList(accountID));
        return !userInfoList.isEmpty();
    }


    /**
     * 获取用户信息
     *
     * @param accountIds
     */
    public List<UserInfo> getUserInfoByHttp(String accountID,List<String> accountIds) {
        Assert.notEmpty(accountIds, "getUserInfoByHttp accountIds 参数不能为空");

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("accountID", accountID);
        requestHeaders.add("tk", USER_TOKEN);

        JSONObject json = new JSONObject();
        json.put("accountIDList", accountIds);

        HttpEntity<String> requestEntity = new HttpEntity<String>(json.toString(), requestHeaders);
        log.info(String.format("发送用户查询消息%s", json));
        String responseJsonStr = httpClientService.commonRequestUtf8(outlet7AAddress.concat("/v2/user/getUser"), HttpMethod.POST, requestEntity, String.class);
        JSONObject jsonObject = JSONObject.parseObject(responseJsonStr);
        if (jsonObject != null) {
            log.info("7A用户信息返回byuser :{}", jsonObject.toJSONString());
        }

        String accountinfListStr = jsonObject.getString("accountInfoList");
        List<UserInfo> userInfoList = new ArrayList<>();
        if (StringUtils.hasText(accountinfListStr)) {
            List<UserInfo> userList = JSONArray.parseArray(accountinfListStr, UserInfo.class);
            userInfoList.addAll(userList);
        }
        return userInfoList;
    }


    /**
     * 获取用户信息
     *
     * @param accountId
     */
    public List<UserInfo> getSingleUserInfoByHttp(String accountId) {
        Assert.notNull(accountId, "getUserInfoByHttp accountId 参数不能为空");
        return getUserInfoByHttp(accountId,Arrays.asList(accountId));
    }

    /**
     * 获取用户信息
     *
     * @param request
     */
    public List<UserInfo> getAccountIdByAccount(UserInfoByUidRequest request) {
        Assert.notEmpty(request.getAccounts(), "getAccountIdByAccount accounts 参数不能为空");

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("accountID", request.getAccountId());
        requestHeaders.add("tk", request.getToken());

        JSONObject json = new JSONObject();
        json.put("accounts", request.getAccounts());

        HttpEntity<String> requestEntity = new HttpEntity<String>(json.toString(), requestHeaders);
        logger.info(String.format("发送用户查询消息%s", json));
        String responseJsonStr = httpClientService.commonRequestUtf8(outlet7AAddress.concat("/v2/user/getUID"), HttpMethod.POST, requestEntity, String.class);
        JSONObject jsonObject = JSONObject.parseObject(responseJsonStr);
        if (jsonObject != null) {
            log.info("7A用户信息返回byuid :{}", jsonObject.toJSONString());
        }

        String accountinfListStr = jsonObject.getString("accountID");
        List<UserInfo> userInfoList = new ArrayList<>();
        if (StringUtils.hasText(accountinfListStr)) {
            List<String> userList = JSONArray.parseArray(accountinfListStr, String.class);
            for (int i = 0, size = userList.size(); i < size; i++) {
                UserInfo userInfo = new UserInfo();
                if (!StringUtils.hasText(userList.get(i))) {
                    continue;
                }
                userInfo.setAccountID(userList.get(i));
                userInfoList.add(userInfo);
            }
        }
        return userInfoList;
    }

    /**
     * 获取用户对语言信息
     *
     * @param fields
     */
    public JSONObject getUserLangInfo(VBMPRequest requestBaseInfo, List<String> fields) {
        Assert.notEmpty(fields, "getUserLangInfo fields 参数不能为空");

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("accountID", requestBaseInfo.getAccountId());
        requestHeaders.add("tk", requestBaseInfo.getToken());

        JSONObject json = new JSONObject();
        json.put("keys", fields);

        HttpEntity<String> requestEntity = new HttpEntity<String>(json.toString(), requestHeaders);
        log.debug(String.format("发送用户多语言信息%s", json));
        String responseJsonStr = httpClientService.commonRequestUtf8(outlet7AAddress.concat("/v1/thirdparty/user/getUserLangInfo"), HttpMethod.POST, requestEntity, String.class);
        JSONObject jsonObject = JSONObject.parseObject(responseJsonStr);
        if (jsonObject != null) {
            log.info(jsonObject.toJSONString());
        }

        return jsonObject;
    }

    /**
     * 同步设备列表
     *
     * @param requestBaseInfo
     * @param accountIDList
     * @return
     */
    public Integer syncUserDevices(VBMPRequest requestBaseInfo, List<String> accountIDList) {
        if(accountIDList == null || accountIDList.isEmpty()){
            return 0;
        }
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Content-Type", "application/json;charset=utf-8");
        requestHeaders.set("tk", requestBaseInfo.getToken());
        requestHeaders.set("accountID", requestBaseInfo.getAccountId());

        JSONObject jsonObjectBody = new JSONObject();
        jsonObjectBody.put("accountIDList", accountIDList);
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonObjectBody.toJSONString(), requestHeaders);
        //转换状态对应, on/off
        String url = outlet7AAddress.concat("/v2/user/updateGHDevList");
        try {
            String responseJsonStr = commonHttpsRequestHeader(url, HttpMethod.POST, requestEntity, String.class);

            if (!StringUtils.hasText(responseJsonStr)) {
                return 0;
            }
            logger.info("syncUserDevices:{}", responseJsonStr);
            JSONObject jsonObject = JSONObject.parseObject(responseJsonStr);
            if(jsonObject != null && jsonObject.getJSONObject("error") != null){
                throw new ServiceException(jsonObject.getJSONObject("error").getInteger("code"), jsonObject.getJSONObject("error").getString("msg"));
            }
        }catch (Exception e){
        	log.info("同步设备列表错误 {}",e);
        }
        return -1;
    }
 
   

    /**
     * 获取header返回错误码的请求方法
     * @param urlPath
     * @param httpMethod
     * @param requestEntity
     * @param responseType
     * @return
     */
    public String commonHttpsRequestHeader(String urlPath, HttpMethod httpMethod, HttpEntity<?> requestEntity, Class<String> responseType) {
    	 ResponseEntity<String> response = null;
         String errorString = null;
         try {
             logger.info("发送http请求url:{}, method:{}, request:{}", urlPath, httpMethod, JSONObject.toJSONString(requestEntity));
             restTemplate.setErrorHandler(new MyResponseErrorHandler());

             //添加utf-8支持
             List<HttpMessageConverter<?>> converterList = restTemplate.getMessageConverters();
             HttpMessageConverter<?> converter = new StringHttpMessageConverter();
             ((StringHttpMessageConverter) converter).setDefaultCharset(Charset.forName("UTF-8"));
             if(!(converterList.get(0) instanceof StringHttpMessageConverter)){
                 converterList.add(0, converter);
                 restTemplate.setMessageConverters(converterList);
             }
             response = restTemplate.exchange(urlPath, httpMethod, requestEntity, responseType);
             logger.info("接收http请求response:{}", JSONObject.toJSONString(response));
         } catch (Exception e) {
             logger.info("发送http请求错误", e);
             throw e;
         }

         if (response != null) {
             errorString = response.getBody();
         }
         return errorString;
    }



}
