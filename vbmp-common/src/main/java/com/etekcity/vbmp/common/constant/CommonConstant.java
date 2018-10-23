package com.etekcity.vbmp.common.constant;

/**
 * @Description: 通用常量类
 * @Author: royle.Huang
 * @Date: 2018/9/7
 */
public class CommonConstant {

    public static final String EMPTY_STRING = "";
    public static final String COMMA_STRING = ",";
    public static final String BLANK_STRING = " ";
    public static final String COLON_STRING = ":";
    public static final String SEMICOLON_STRING = ";";
    public static final String ADD = "add";
    public static final String DEL = "del";
    public static final String UPD = "upd";
    public static final String AT = "@";

    public static final int COMMON_SUCCESS = 0;

    public static final String COMMON_STATUS_ON = "on";
    public static final String COMMON_STATUS_OFF = "off";
    public static final String COMMON_INT_STATUS_ON = "1";
    public static final String COMMON_INT_STATUS_OFF = "0";

    public static final String COMMON_CONNECTION_STATUS_ONLINE = "online";
    public static final String COMMON_CONNECTION_STATUS_OFFLINE = "offline";

    public static final Long SECONDS_OF_ONEDAY = 24L * 60L * 60L;

    public static final String GOOGLE_HOME_CID_NIGHT_LIGHTT = "_nightlight";
    public static final String REDIS_KEY_DEVICE_PREFIX = "device:";

    public static final String FCM_SHARE_ADD = "share";
    public static final String FCM_SHARE_DEL = "unShare";

    // ---------------------------------redis-------------------------------------
    public static final String REDIS_KEY_DEVICE_UUID = "vbmp-common:device:uuid:";
    public static final String REDIS_KEY_DEVICE_CID = "vbmp-common:device:cid:";
    public static final String UUID_DEVICE_OBJECT = "vbmp-common:object";
    public static final String REDIS_FIELD_USED_DEVICE_TYPE = "useddevicetype";
    public static final String REDIS_KEY_DEVICE_SHARE = "vbmp-common:device:share";
    public static final String REDIS_KEY_DEVICE_SHARE_HISTORY = "vbmp-common:device:share:history";
    public static final String REDIS_KEY_DEVICE_SHARE_USER = "vbmp-common:device:share:accountid";
    public static final String REDIS_KEY_WIFIOUTLET_UUID = "vbmp-common:device:wifioutlet:uuid:";
    public static final String REDIS_KEY_WIFIOUTLET_CID = "vbmp-common:device:wifioutlet:cid";
    public static final String REDIS_KEY_PLANTE_ACCONUTID_PREFIX = "plante:account:accountid:";
    public static final String REDIS_KEY_COMMON_FUNC_PREFIX = "vbmp-common:device-commom";
    public static final String REDIS_KEY_DEVICE_OWN_ACCOUNT = "vbmp-common:device:own:accountid:";
    public static final String REDIS_KEY_DEVICE_SHARE_ACCOUNT = "vbmp-common:device:share:accountid:";

    // ---------------------------------user----------------------------------------
    public static String UserHash = "w:u:";


}
