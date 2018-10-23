package com.etekcity.vbmp.timing.filter.aop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.timing.common.VBMPResponse;
import com.etekcity.vbmp.timing.common.redis.RedisService;
import com.etekcity.vbmp.timing.common.service.DeviceInfoService;
import com.etekcity.vbmp.timing.common.service.impl.HttpClientService;
import com.etekcity.vbmp.timing.constant.CommonConstant;
import com.etekcity.vbmp.timing.constant.ErrorConstant;
import com.etekcity.vbmp.timing.exception.CalibrationException;
import com.etekcity.vbmp.timing.util.MyJsonUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;

/**
 * AspectJ控制组件
 * 用于aop拦截方法
 */
@Component
@Aspect
public class AspectComponent {

    private static Logger logger = LoggerFactory.getLogger(AspectComponent.class);

    @Autowired
    RestTemplate restTemplate;
    @Autowired
    RedisService redisService;
    @Autowired
    DeviceInfoService deviceInfoService;
    @Autowired
    HttpClientService httpClientService;


    @Value("${token.remoteAddress}")
    private String address;
    @Value("${token.redisdb}")
    private int db;
    @Value("${token.expireTime}")
    private long expireTime;

    /**
     * 处理对外接口的返回数据
     *
     * @param point
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.etekcity.vbmp.timing.filter.aop.ApplicationResponse)")
    public Object before(ProceedingJoinPoint point) throws Throwable {
        Object res = null;
        try {
            res = point.proceed();
        } catch (CalibrationException e) {
            res = new VBMPResponse(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            res = new VBMPResponse(ErrorConstant.ERR_SERVER_UNKNOWN, e.getMessage());
        }
        return res;
    }


    /**
     * 校验参数
     *
     * @param point
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.etekcity.vbmp.timing.filter.aop.Calibration)")
    public Object check(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Object[] args = point.getArgs();
        if (args.length < 1) {
            throw new CalibrationException(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG, ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
        }
        JSONObject param = null;
        if (args[0] instanceof String) {
            param = JSON.parseObject((String) args[0]);
        } else if (args[0] instanceof JSONObject) {
            param = (JSONObject) args[0];
        } else {
            param = JSON.parseObject(JSON.toJSONString(args[0]));
        }
        if (param == null) {
            throw new CalibrationException(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG, ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
        }
        Calibration calibration = method.getAnnotation(Calibration.class);
        String[] fields = calibration.fields();
        for (String field : fields) {
            if (!param.containsKey(field) || param.get(field) == null || "".equals(param.get(field))) {
                logger.info("参数错误，".concat(field).concat("为空"));
                throw new CalibrationException(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG, ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            }
        }
        if (calibration.checkToken()) {
            String token = param.getString("token");
            String accountId = param.getString("accountId");
            if (token == null || accountId == null) {
                throw new CalibrationException(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG, ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            }
            if (!tokenValidation(token, accountId)) {
                throw new CalibrationException(ErrorConstant.ERR_ACCOUNT_OR_PASSOWRD_WRONG_MSG, ErrorConstant.ERR_ACCOUNT_OR_PASSWORD_WRONG);
            }
        }
        if (calibration.checkDevice()) {
            String uuid = param.getString("uuid");
            if (uuid == null) {
                throw new CalibrationException(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG, ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            }
            if (deviceInfoService.findDeviceByUuid(uuid) == null) {
                throw new CalibrationException(ErrorConstant.ERR_DEVICE_NOT_EXIST_MSG, ErrorConstant.ERR_DEVICE_NOT_EXIST);
            }
        }
        if (calibration.checkOnline()) {
            String uuid = param.getString("uuid");
            if (uuid == null) {
                throw new CalibrationException(ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG, ErrorConstant.ERR_REQUEST_PARAM_EMPTY);
            }
            JSONObject jsonObject = httpClientService.getDeviceStatus(uuid);
            if (jsonObject.getInteger("code") != 0) {
                throw new CalibrationException(jsonObject.getString("msg"), jsonObject.getInteger("code"));
            }
            String deviceStatus = MyJsonUtils.getJsonInfo(JSON.parseObject(jsonObject.getString("jsonStr")), "data", "payload", "state", "reported", "connectionStatus").toString();
            if (!CommonConstant.COMMON_CONNECTION_STATUS_ONLINE.equals(deviceStatus)) {
                throw new CalibrationException(ErrorConstant.ERR_CONTROLLER_OFFLINE_MSG, ErrorConstant.ERR_CONTROLLER_OFFLINE);
            }
        }
        Object res = point.proceed();
        return res;
    }


    /**
     * token验证
     *
     * @param token
     * @param accountId
     * @return
     */
    public boolean tokenValidation(String token, String accountId) {
        try {
            //redis验证，减少并发
            if (accountId.equals(redisService.get(token, db))) {
                return true;
            }
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add("tk", token);
            requestHeaders.add("accountID", accountId);
            HttpEntity<String> requestEntity = new HttpEntity<>(null, requestHeaders);
            ResponseEntity<String> response = restTemplate.exchange(address.concat("/token"),
                    HttpMethod.GET, requestEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                redisService.set(token, accountId, expireTime, db);
                return true;
            } else {
                logger.info(JSONObject.toJSONString(response));
            }
        } catch (Exception e) {
            logger.info("鉴权异常", e);
        }
        return false;
    }
}
