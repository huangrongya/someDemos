package com.etekcity.vbmp.device.control.service.ServiceImpl;

import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.device.control.constant.ErrorConstant;
import com.etekcity.vbmp.device.control.dao.mapper.DeviceGetControlMapper;
import com.etekcity.vbmp.device.control.dao.model.DeviceGetConrtol;
import com.etekcity.vbmp.device.control.dao.model.DeviceType;
import com.etekcity.vbmp.device.control.dto.ErrorResponse;
import com.etekcity.vbmp.device.control.dto.GetConrtolRequest;
import com.etekcity.vbmp.device.control.dto.GetConrtolResponse;
import com.etekcity.vbmp.device.control.dto.InsertConrtolRequest;
import com.etekcity.vbmp.device.control.service.DeviceGetConrtolService;
import com.etekcity.vbmp.device.control.utils.MyHttpsUtils;
import com.etekcity.vbmp.device.control.utils.MyStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DeviceGetConrtolServiceImpl implements DeviceGetConrtolService {

    private Logger logger = LoggerFactory.getLogger(DeviceGetConrtolServiceImpl.class);

    @Autowired
    private DeviceGetControlMapper deviceGetControlMapper;

    @Autowired
    RestTemplate restTemplate;

    /*@Value("${openapi.shadow.address}")
    private String address;*/
  /*  @Value("${openapi.accessid}")
    private String accessId;*/
   /* @Value("${openapi.accesskey}")
    private String accessKey;
    @Value("${openapi.authkey}")
    private String authkey;*/

    private static ResponseErrorHandler errorHander = new DefaultResponseErrorHandler();

    @Override
    public GetConrtolResponse getConrtolService(GetConrtolRequest request) throws Exception {

        //获取数据库中通用协议接口
        DeviceGetConrtol dgc = deviceGetControlMapper.getAgreement(request.getIdentify());
        DeviceType getParameters = deviceGetControlMapper.getParameters(request.getConfigModel());
        GetConrtolResponse response = new GetConrtolResponse();
        if (dgc.getAgreement() == null && dgc.getAgreement().isEmpty()) {
            response.setCode(ErrorConstant.ERR_REQUEST_NOT_AGREEMENT);
            response.setMsg(ErrorConstant.ERR_REQUEST_NOT_AGREEMENT_MSG);
            logger.error("通用协议为null");
            return response;
        }
        if (MyStringUtils.isNullData(getParameters.getAccessId(), getParameters.getAccessKey(), getParameters.getAddress(), getParameters.getAuthkey()
        )) {
            response.setCode(ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            response.setMsg(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            logger.error("deviceType数据库查询数据为null");
            return response;
        }
        String jsonStr = getJsonStr(request, dgc, getParameters);
        response.setJsonStr(jsonStr);
        return response;
    }

    @Override
    public ErrorResponse insetConrtolService(InsertConrtolRequest request) throws Exception {
        ErrorResponse response = new ErrorResponse();
        deviceGetControlMapper.insetConrtolMapper(request);
        return response;
    }

    public String getJsonStr(GetConrtolRequest request, DeviceGetConrtol dgc, DeviceType getParameters) {
        String agreement = dgc.getAgreement();
        String uuid = request.getUuid();
        Object[] msg = request.getMsg();
        //循环用户参数，并将用户参数替换到通用协议接口中
        if (request.getMsg() != null && request.getMsg().length > 0) {
            for (int i = 0; i < msg.length; i++) {
                String idex = "{" + i + "}";
                String indexMsg = msg[i].toString();
                if(!StringUtils.hasText(indexMsg)){
                    indexMsg = "null";
                }
                agreement = agreement.replace(idex, indexMsg);
            }
            JSONObject agreementJO = JSONObject.parseObject(agreement);
            Map<String, Object> maps = (Map<String, Object>) agreementJO;
            //getMap(maps);
            agreement = maps.toString();
        }
        //调用VDMP参数
        /*String mapString = dgc.getParamMap();
        JSONObject jasonObject = JSONObject.parseObject(mapString);
        Map<String, Object> map = (Map<String, Object>) jasonObject;
        Map<String, String> paramMap = new TreeMap<>();
        for (String key : map.keySet()) {
            paramMap.put(key, map.get(key).toString());
            //System.out.println(key + "=>" + testMap2.get(key));
        }*/
        Map<String,Object> map = new TreeMap<>();
        if (request.getParamMap() != null && request.getParamMap().size() > 0) {
            map.putAll(request.getParamMap());
        }
        map.put("uuid", uuid);
        map.put("authKey", getParameters.getAuthkey());
        String url = dgc.getUrl();
        String path = MyHttpsUtils.getMd5RestRequestParamsStr(map, getParameters.getAccessId(), getParameters.getAccessKey());
        String urlPath = getParameters.getAddress() + url.concat(path);
        logger.info(String.format("请求地址:%s", urlPath));

        //判断参数，并调用
        String httpMethodID = dgc.getHttpMethodID();
        String jsonStr;
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> requestEntity = new HttpEntity<>(agreement, requestHeaders);
        if (httpMethodID.equals("get")) {
            jsonStr = commonHttpsRequest(urlPath, HttpMethod.GET, null, String.class);
        } else if (httpMethodID.equals("head")) {
            jsonStr = commonHttpsRequest(urlPath, HttpMethod.HEAD, requestEntity, String.class);
        } else if (httpMethodID.equals("post")) {
            jsonStr = commonHttpsRequest(urlPath, HttpMethod.POST, requestEntity, String.class);
        } else if (httpMethodID.equals("put")) {
            jsonStr = commonHttpsRequest(urlPath, HttpMethod.PUT, requestEntity, String.class);
        } else if (httpMethodID.equals("patch")) {
            jsonStr = commonHttpsRequest(urlPath, HttpMethod.PATCH, requestEntity, String.class);
        } else if (httpMethodID.equals("delete")) {
            jsonStr = commonHttpsRequest(urlPath, HttpMethod.DELETE, null, String.class);
        } else if (httpMethodID.equals("options")) {
            jsonStr = commonHttpsRequest(urlPath, HttpMethod.OPTIONS, requestEntity, String.class);
        } else if (httpMethodID.equals("trace")) {
            jsonStr = commonHttpsRequest(urlPath, HttpMethod.TRACE, requestEntity, String.class);
        } else {
            jsonStr = "";
        }

        return jsonStr;
    }

    public void getMap(Map<String,Object> maps){
        //System.out.println("=====删除前=====\n"+maps);
        logger.info("=====删除前=====",maps.toString());
        Iterator<String> iter = maps.keySet().iterator();
        while(iter.hasNext()){
            String key = iter.next();
            if("null".equals(maps.get(key)) || maps.get(key) == null){
               // System.out.println("123");
                iter.remove();
            }
            if(maps.get(key) instanceof java.util.Map){
               // System.out.println("456");
                getMap((Map<String, Object>) maps.get(key));
            }
        }
    }

    public String commonHttpsRequest(String urlPath, HttpMethod httpMethod, HttpEntity<?> requestEntity, Class<String> responseType) {
        ResponseEntity<String> response = null;
        String responseString = null;
        try {
            restTemplate.setErrorHandler(errorHander);
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
        if (response != null && response.getHeaders().containsKey("error")) {
            logger.warn("请求出错,错误码：{}", JSONObject.toJSONString(response.getHeaders().get("error")));
        }
        if (response != null) {
            responseString = response.getBody();
        }
        if (responseString == null) {
            responseString = "";
        }
        return responseString;
    }
}
