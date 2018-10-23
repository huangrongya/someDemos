package com.etekcity.vbmp.timing.constant;

public class CommonConstant {

    /**
     * 空字符串
     */
    public static final String EMPTY_STRING = "";
    /**
     * 逗号字符串
     */
    public static final String COMMA_STRING = ",";

    /**
     * 空格字符串
     */
    public static final String BLANK_STRING = " ";

    private static final String MODEL_PREFIX = "vbmp-common:";
    public static final String DEVICE_TYPE_REDIS_KEY = "vbmp-common:device:type";

    public static final String REDIS_KEY_DEVICE_PREFIX = MODEL_PREFIX.concat("device:");

    public static final String REDIS_KEY_ACCOUNT_PREFIX = MODEL_PREFIX.concat("account:accountid:");

    public static final String REDIS_KEY_DEVICE_UUID_PREFIX = MODEL_PREFIX.concat("device:deviceuuid:");

    public static final String REDIS_KEY_PLANTE_ACCONUTID_PREFIX = "plante:account:accountid:";

    public static final String REDIS_KEY_COMMON_FUNC_PREFIX = "device-commom";

    public static final String UUID_DEVICE_OBJECT = "object";

    public static final String ACCOUNT_OWN_DEVICES = "owndevices";

    public static final String ACCOUNT_SHARED_DEVICES = "shareddevices";

    public static final String CID_TO_UUID = "cidtouuid";

    public static final String HISTORY_SHAREPEOPLE = "sharepeople-history";

    public static final String HISTORY_DEVICENAME = "devicename-history";

    public static final String REDIS_KEY_DEVICE_MOBILE = "device:".concat(MODEL_PREFIX).concat("uuid-mobileid");

    public static final String REDIS_FIELD_DEVICE_STATUS = "devicestatus";

    public static final String REDIS_FIELD_NIGHT_LIGHT_STATUS = "nightlightstatus";

    public static final String REDIS_FIELD_NIGHT_LIGHT_AUTO_MODE = "nightlightAutoMode";

    public static final String REDIS_FIELD_CONNECTIONSTATUS = "connectionStatus";

    public static final String REDIS_FIELD_POWER = "power";

    public static final String REDIS_FIELD_VOLTAGE = "voltage";

    public static final String REDIS_FIELD_USED_DEVICE_TYPE = "useddevicetype";

    public static final Long SECONDS_OF_ONEDAY = 24L * 60L * 60L;

    public static final int COMMON_SUCCESS = 0;

    public static final String COMMON_STATUS_ON = "on";

    public static final String COMMON_STATUS_OFF = "off";

    public static final String COMMON_STATUS_AUTO = "auto";

    public static final String COMMON_STATUS_MANUAL = "manual";

    public static final String COMMON_STATUS_SLEEP = "sleep";

    public static final String COMMON_INT_STATUS_ON = "1";

    public static final String COMMON_INT_STATUS_OFF = "0";

    public static final String COMMON_CONNECTION_STATUS_ONLINE = "online";

    public static final String COMMON_CONNECTION_STATUS_OFFLINE = "offline";


    public static final String API_DEVICE_STATUS_OPEN = "open";
    public static final String API_DEVICE_STATUS_BREAK = "break";
    public static final String API_DEVICE_STATUS_ON = "on";
    public static final String API_DEVICE_STATUS_OFF = "off";
    public static final String MONGODB_ENERGY_COLLECTION_NAME = "energy";
    public static final String MONGODB_ENERGY_SINGLE_USE_COLLECTION_NAME = "energySingleUse";


    public static final String TIMING_AWAY = REDIS_KEY_DEVICE_PREFIX.concat("aways");
    public static final String TIMING_TIMER = REDIS_KEY_DEVICE_PREFIX.concat("timers");
    public static final String TIMING_SCHEDULE = REDIS_KEY_DEVICE_PREFIX.concat("schedules");
    public static final String TIMING_LIGHT_SCHEDULE = REDIS_KEY_DEVICE_PREFIX.concat("light-schedules");

    public static final String START = "S";
    public static final String END = "E";

    public static final String VDMP_DEVICE_SCHEDULE_USED_PREFIX = "vbmp-common:vdmp:schedule:used";
    public static final String VDMP_DEVICE_SCHEDULE_UNUSED_PREFIX = "vbmp-common:vdmp:schedule:unused";
    public static final String VDMP_DEVICE_TIMER_USED_PREFIX = "vbmp-common:vdmp:timer:used";
    public static final String VDMP_DEVICE_TIMER_UNUSED_PREFIX = "vbmp-common:vdmp:timer:unused";

    public static final String DEVICE_LIGHT_SCHEDULE_MAP_PREFIX = "vbmp-common:timing:light-schedule";
    public static final String DEVICE_SCHEDULE_MAP_PREFIX = "vbmp-common:timing:schedule";
    public static final String DEVICE_TIMER_MAP_PREFIX = "vbmp-common:timing:timer";

    public static final String DEVICE_AWAY_LOCK_PREFIX = "vbmp-common:lock:away:";
    public static final String DEVICE_SCHEDULE_LOCK_PREFIX = "vbmp-common:lock:schedule:";
    public static final String DEVICE_TIMER_LOCK_PREFIX = "vbmp-common:lock:timer:";

    public static final String ADD = "add";
    public static final String DEL = "del";
    public static final String UPD = "upd";

    public static final String AT = "@";

    public static final String TRIGGER = "trigger";
    public static final String TYPE = "type";

    public static final String STATE_TOPIC = "Devices/vbmp-commonAStatus/PTP";
    public static final String ELECTRIC_TOPIC = "Devices/vbmp-commonAData/PTP";

    public static final String GoogleHomeCidNightLightTail = "_nightlight";

    public static long OFFSET_TIME = 1L; // 分

    /****************空气净化器档位begin  hry*****************/
    public static final String AIRPURIFIER_LEVEL_LOW = "LOW";
    public static final String AIRPURIFIER_LEVEL_MEDIUM = "MEDIUM";
    public static final String AIRPURIFIER_LEVEL_HIGH = "HIGH";
    /****************空气净化器档位end   hry*****************/

    public static final String EVENT_SWITCH = "switch";
    public static final String EVENT_LIGHT = "nightlight";
}
