package com.etekcity.vbmp.device.control.utils;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Map;

public class MyJsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(MyJsonUtils.class);

    /**
     * @return com.alibaba.fastjson.JSONObject
     * @Author Ericer
     * @Description json参数设置
     * @Date 上午11:22 18-9-18
     * @Param [jsonObject, mapParam]
     **/
    public static JSONObject setJsonValue(JSONObject jsonObject, Map<String, Object> mapParam) {
        Assert.notNull(jsonObject, "setJsonValue jsonObject不能为空");
        if (mapParam == null || mapParam.isEmpty()) {
            return jsonObject;
        }

        JSONObject jsonObjectReturn = JSONObject.parseObject(jsonObject.toJSONString());

        for (String key : jsonObjectReturn.keySet()) {
            Object object = jsonObjectReturn.get(key);
            if (object instanceof JSONObject) {
                JSONObject jsonObject2 = (JSONObject) object;
                jsonObject2 = setJsonValue(jsonObject2, mapParam);
                jsonObjectReturn.put(key, jsonObject2);
            } else {
                if (mapParam.containsKey(key)) {
                    jsonObjectReturn.put(key, mapParam.get(key));
                } else {
                    jsonObjectReturn.remove(key);
                }
            }
        }
        return jsonObjectReturn;
    }

}
