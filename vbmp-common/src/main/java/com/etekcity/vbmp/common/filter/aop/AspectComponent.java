package com.etekcity.vbmp.common.filter.aop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.config.redis.RedisService;
import com.etekcity.vbmp.common.constant.ErrorConstant;
import com.etekcity.vbmp.common.exception.CalibrationException;
import com.etekcity.vbmp.common.exception.ServiceException;
import com.etekcity.vbmp.common.utils.MyStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
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
@Slf4j
public class AspectComponent {

    @Autowired
    RestTemplate restTemplate;
    @Autowired
    RedisService redisService;

    @Value("${token.remoteAddress}")
    private String address;
    @Value("${token.redisdb}")
    private int db;
    @Value("${token.expireTime}")
    private long expireTime;

    /**
     * 校验参数
     *
     * @param point
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.etekcity.vbmp.common.filter.aop.Calibration)")
    public Object check(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Object[] args = point.getArgs();
        if (args.length < 1) {
            throw new CalibrationException(ErrorConstant.ERR_REQUEST_PARAM_EMPTY, ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
        }
        JSONObject param = null;
        if (args[0] instanceof String) {
            param = JSON.parseObject((String) args[0]);
        }
        if (args[0] instanceof JSONObject) {
            param = (JSONObject) args[0];
        }
        if (param == null) {
            throw new CalibrationException(ErrorConstant.ERR_INVALID_PARAM_FORMAT, ErrorConstant.ERR_INVALID_PARAM_FORMAT_MSG);
        }
        Calibration calibration = method.getAnnotation(Calibration.class);
        String[] fields = calibration.fields();
        for (String field : fields) {
            if (!param.containsKey(field) || "".equals(param.get(field))) {
                log.info("参数错误，".concat(field).concat("为空"));
                return new CalibrationException(ErrorConstant.ERR_REQUEST_PARAM_EMPTY, ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            }
        }
        if (calibration.checkToken()) {
            String token = param.getString("token");
            String accountId = param.getString("accountId");
            if (MyStringUtils.isNullData(token, accountId)) {
                throw new ServiceException(ErrorConstant.ERR_REQUEST_PARAM_EMPTY, ErrorConstant.ERR_REQUEST_PARAM_EMPTY_MSG);
            }
            if (!tokenValidation(token, accountId)) {
                throw new CalibrationException(ErrorConstant.ERR_ACCOUNT_OR_PASSWORD_WRONG, ErrorConstant.ERR_ACCOUNT_OR_PASSOWRD_WRONG_MSG);
            }
        }
        Object res = point.proceed();
        return res;
    }

    /**
     * 处理对外接口的返回数据
     *
     * @param point
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.etekcity.vbmp.common.filter.aop.ApplicationResponse)")
    public Object response(ProceedingJoinPoint point) throws Throwable {
        Object res;
        try {
            res = point.proceed();
        } catch (CalibrationException e) {
            res = new VBMPResponse(e.getCode(), e.getMessage());
        } catch (Exception e) {
            res = new VBMPResponse(1, "");
        }
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
            Object tokenValue = redisService.get(token, db);
            if (accountId.equals(tokenValue)) {
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
                log.info(JSONObject.toJSONString(response));
            }
        } catch (Exception e) {
            log.info("鉴权异常");
        }
        return false;
    }
}
