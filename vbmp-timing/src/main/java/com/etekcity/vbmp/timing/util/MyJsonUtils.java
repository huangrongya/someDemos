package com.etekcity.vbmp.timing.util;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class MyJsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(MyJsonUtils.class);

    public static String DEVICESTATUS = "deviceStatus";
    public static String CONNECTIONSTATUS = "connectionStatus";
    public static String POWER = "power";

    /**
     * 根据key对应键值
     *
     * @param jsonObject
     * @param keys
     * @return
     */
    public static Object getJsonInfo(JSONObject jsonObject, String... keys) {
        Object object = null;
        if (jsonObject == null) {
            return object;
        }
        JSONObject jsonObjectTemp = new JSONObject(jsonObject);
        for (int i = 0, size = keys.length; i < size; i++) {
            if (jsonObjectTemp.get(keys[i]) != null) {
                if (jsonObjectTemp.get(keys[i]) instanceof JSONObject) {
                    jsonObjectTemp = (JSONObject) jsonObjectTemp.get(keys[i]);
                    if (i == size - 1) {
                        object = jsonObjectTemp;
                    }
                } else if (i == size - 1) {
                    object = jsonObjectTemp.get(keys[i]);
                } else {
                    logger.debug("未查找到对应的对象");
                    break;
                }
            } else {
                break;
            }
        }
        return object;
    }

    /**
     * 将设备状态相关的数据转为map
     *
     * @param jSONObject
     * @return
     */
    public static Map<String, Object> coverDeviceStatusToMap(JSONObject jSONObject) {
        Map<String, Object> map = new HashMap<>(7);
        if (jSONObject == null) {
            return map;
        }
        JSONObject stateJSONObject = (JSONObject) getJsonInfo(jSONObject, "data", "payload", "state");
        // 获取设备开关状态
        Object powerOn;
        // jSONObjectStatus 存储 开关机 on/off
        if ((powerOn = getJsonInfo(stateJSONObject, "reported", "powerOn")) != null) {
            map.put(DEVICESTATUS, powerOn);
        }
        Object connectionStatus;
        // jSONObjectStatus 存储 在线状态字段值 online/offline
        if ((connectionStatus = getJsonInfo(stateJSONObject, "reported", "connectionStatus")) != null) {
            map.put("connectionStatus", connectionStatus);
        }

        Object screen;
        // jSONObjectStatus 存储 屏幕显示值 on/off
        if ((screen = getJsonInfo(stateJSONObject, "reported", "screen")) != null) {
            map.put("screen", screen);
        }

        Object change;
        // jSONObjectStatus 存储 滤网状态 ”false” 无需更换， “true” 需要更换
        if ((change = getJsonInfo(stateJSONObject, "reported", "filterLife", "change")) != null) {
            map.put("change", change);
        }
        Object hour;
        // jSONObjectStatus 存储 滤网状态 剩余小时
        if ((hour = getJsonInfo(stateJSONObject, "reported", "filterLife", "hour")) != null) {
            map.put("hour", hour);
        }
        Object percent;
        // jSONObjectStatus 滤网复位 on/off
        if ((percent = getJsonInfo(stateJSONObject, "reported", "filterLife", "percent")) != null) {
            map.put("percent", percent);
        }
        Object filterReset;
        // jSONObjectStatus 滤网复位 on/off
        if ((filterReset = getJsonInfo(stateJSONObject, "reported", "filterReset")) != null) {
            map.put("filterReset", filterReset);
        }
        Object airQualityLevel;
        // jSONObjectStatus 存储 空气质量
        if ((airQualityLevel = getJsonInfo(stateJSONObject, "reported", "airQualityLevel")) != null) {
            map.put("airQuality", airQualityLevel);
        }
        Object runTime;
        // jSONObjectStatus 存储 本次开机运行时间 单位: 秒
        if ((runTime = getJsonInfo(stateJSONObject, "reported", "runTime")) != null) {
            map.put("runTime", runTime);
        }
        Object level;
        // jSONObjectStatus 存储 净化器风速档位 1,2,3
        if ((level = getJsonInfo(stateJSONObject, "reported", "purifierMode", "level")) != null) {
            map.put("level", level);
        }
        Object purifierMode;
        // jSONObjectStatus 存储 模式功能 auto|sleep|manual
        if ((purifierMode = getJsonInfo(stateJSONObject, "reported", "purifierMode", "mode")) != null) {
            map.put("mode", purifierMode);
        }
        //
        Object uuid;
        if ((uuid = getJsonInfo(jSONObject, "uuid")) != null) {
            map.put("uuid", uuid);
        }
        //
        Object cid;
        if ((cid = getJsonInfo(stateJSONObject, "reported", "_id")) != null) {
            map.put("cid", cid);
        }
        //
        Object firmware;
        if ((firmware = getJsonInfo(stateJSONObject, "reported", "firmware")) != null) {
            map.put("firmware", firmware);
        }
        //
        Object currentFirmVersion;
        if ((currentFirmVersion = getJsonInfo(stateJSONObject, "reported", "version", "firmVersion")) != null) {
            map.put("currentFirmVersion", currentFirmVersion);
        }

        //正在运行的schedule
        String scheduleRunning;
        if ((scheduleRunning = (String) getJsonInfo(stateJSONObject, "reported", "scheduleRunning")) != null) {
            map.put("scheduleRunning", scheduleRunning);
        }
        //timer状态
        String timerAction;
        if ((timerAction = (String) getJsonInfo(stateJSONObject, "reported", "timer", "action")) != null) {
            map.put("timerAction", timerAction);
        }
        //timer剩余秒数
        Integer second;
        if ((second = (Integer) getJsonInfo(stateJSONObject, "reported", "timer", "second")) != null) {
            map.put("second", second);
        }

        return map;
    }

    /**
     * kafka消息处理
     *
     * @param jsonObject
     * @return
     */
    public static JSONObject getDataJsonObject(JSONObject jsonObject) {
        String dataJson = (String) MyJsonUtils.getJsonInfo(jsonObject, "data");
        JSONObject dataJsonObject = null;
        if (StringUtils.hasLength(dataJson)) {
            dataJsonObject = JSONObject.parseObject(dataJson);
        }
        return dataJsonObject;
    }

    public static Map<String, Object> coverDeviceStatusKafKaToMap(JSONObject jSONObject) {
        Map<String, Object> map = new HashMap<>(7);
        if (jSONObject == null) {
            return map;
        }
        Object uuid;
        if ((uuid = jSONObject.getString("uuid")) != null) {
            map.put("uuid", uuid);
        }


        JSONObject stateJSONObject = getDataJsonObject(jSONObject);
        stateJSONObject = (JSONObject) getJsonInfo(stateJSONObject, "state");
        // 获取设备开关状态
        Object powerOn;
        // jSONObjectStatus 存储 开关机 on/off
        if ((powerOn = getJsonInfo(stateJSONObject, "reported", "powerOn")) != null) {
            map.put(DEVICESTATUS, powerOn);
        }
        Object connectionStatus;
        // jSONObjectStatus 存储 在线状态字段值 online/offline
        if ((connectionStatus = getJsonInfo(stateJSONObject, "reported", "connectionStatus")) != null) {
            map.put("connectionStatus", connectionStatus);
        }

        Object screen;
        // jSONObjectStatus 存储 屏幕显示值 on/off
        if ((screen = getJsonInfo(stateJSONObject, "reported", "screen")) != null) {
            map.put("screen", screen);
        }

        Object change;
        // jSONObjectStatus 存储 滤网状态 ”false” 无需更换， “true” 需要更换
        if ((change = getJsonInfo(stateJSONObject, "reported", "filterLife", "change")) != null) {
            map.put("change", change);
        }
        Object hour;
        // jSONObjectStatus 存储 滤网状态 剩余小时
        if ((hour = getJsonInfo(stateJSONObject, "reported", "filterLife", "hour")) != null) {
            map.put("hour", hour);
        }
        Object percent;
        // jSONObjectStatus 滤网复位 on/off
        if ((percent = getJsonInfo(stateJSONObject, "reported", "filterLife", "percent")) != null) {
            map.put("percent", percent);
        }
        Object filterReset;
        // jSONObjectStatus 滤网复位 on/off
        if ((filterReset = getJsonInfo(stateJSONObject, "reported", "filterReset")) != null) {
            map.put("filterReset", filterReset);
        }
        Object airQualityLevel;
        // jSONObjectStatus 存储 空气质量
        if ((airQualityLevel = getJsonInfo(stateJSONObject, "reported", "airQualityLevel")) != null) {
            map.put("airQuality", airQualityLevel);
        }
        Object runTime;
        // jSONObjectStatus 存储 本次开机运行时间 单位: 秒
        if ((runTime = getJsonInfo(stateJSONObject, "reported", "runTime")) != null) {
            map.put("runTime", runTime);
        }
        Object level;
        // jSONObjectStatus 存储 净化器风速档位 1,2,3
        if ((level = getJsonInfo(stateJSONObject, "reported", "purifierMode", "level")) != null) {
            map.put("level", level);
        }
        Object purifierMode;
        // jSONObjectStatus 存储 模式功能 auto|sleep|manual
        if ((purifierMode = getJsonInfo(stateJSONObject, "reported", "purifierMode", "mode")) != null) {
            map.put("mode", purifierMode);
        }
        //
        Object cid;
        if ((cid = getJsonInfo(stateJSONObject, "reported", "_id")) != null) {
            map.put("cid", cid);
        }
        //
        Object firmware;
        if ((firmware = getJsonInfo(stateJSONObject, "reported", "firmware")) != null) {
            map.put("firmware", firmware);
        }
        //
        Object currentFirmVersion;
        if ((currentFirmVersion = getJsonInfo(stateJSONObject, "reported", "version", "firmVersion")) != null) {
            map.put("currentFirmVersion", currentFirmVersion);
        }

        //正在运行的schedule
        String scheduleRunning;
        if ((scheduleRunning = (String) getJsonInfo(stateJSONObject, "reported", "scheduleRunning")) != null) {
            map.put("scheduleRunning", scheduleRunning);
        }
        String restore;
        if ((restore = (String) getJsonInfo(stateJSONObject, "reported", "restore")) != null) {
            map.put("restore", restore);
        }
        return map;
    }
}
