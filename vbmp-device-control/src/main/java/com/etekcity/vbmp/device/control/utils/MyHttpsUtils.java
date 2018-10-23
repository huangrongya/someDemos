package com.etekcity.vbmp.device.control.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class MyHttpsUtils {

    private static String URL_SPLIT_SYMBLE = "&";

    private static Logger logger = LoggerFactory.getLogger(MyHttpsUtils.class);

    /**
     * 加密验证签名，并返回组装好的字符串路径
     *
     * @param filedMap
     * @param accessId
     * @param accessKey
     * @return
     * @throws IllegalStateException
     */
    public static String getMd5RestRequestParamsStr(Map<String, Object> filedMap, String accessId, String accessKey) throws IllegalStateException {
        if (filedMap == null) {
            filedMap = new TreeMap<>();
        }
        if (!filedMap.getClass().equals(TreeMap.class)) {
            throw new IllegalStateException("不支持的参数类型，需要TreeMap");
        }
        Assert.hasLength(accessId, "accessId参数不能为空！");
        Assert.hasLength(accessKey, "accesskey参数不能为空！");
        //添加AccessKey，Timestamp
        filedMap.put("accessID", accessId);
        String timeStamp = String.valueOf(new Date().getTime());
        filedMap.put("timestamp", timeStamp);
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entity : filedMap.entrySet()) {
            if (entity.getValue() != null) {
                stringBuilder.append(entity.getKey()).append("=").append(entity.getValue()).append(URL_SPLIT_SYMBLE);
            }
        }
        String pathNoAccessKey = stringBuilder.toString();
        //accessKey不参与排序，直接加到末尾
        stringBuilder.append("accessKey").append("=").append(accessKey);

        //计算Sign，并添加请求路径中
        String str = stringBuilder.toString();
        String md5str = org.springframework.util.DigestUtils.md5DigestAsHex(str.getBytes());

        String path = pathNoAccessKey.concat("sign=").concat(md5str.toUpperCase());
        return path;
    }

    public static String getHost() {
        String host = "";
        try {
            host = InetAddress.getLocalHost().getHostAddress();
            logger.info("current host is:{}", host);
        } catch (UnknownHostException e) {
            logger.error("get host has error", e);
        }
        return host;
    }
}
