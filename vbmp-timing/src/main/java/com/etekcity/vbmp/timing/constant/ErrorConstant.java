package com.etekcity.vbmp.timing.constant;

/**
 * @Description: 错误码
 * @Author: royle.Huang
 * @Date: 2018/9/7
 */
public class ErrorConstant {

    public static final int ERR_SERVER_UNKNOWN = 40011000;
    public static final int ERR_REQUEST_PARAM_EMPTY = 40011001;
    public static final String ERR_REQUEST_PARAM_EMPTY_MSG = "参数为空";
    public static final int ERR_INVALID_ACCOUNT_FORMAT = 40011002;
    public static final String ERR_INVALID_ACCOUNT_FORMAT_MSG = "账户格式错误";
    public static final int ERR_INVALID_PASSWORD_FORMAT = 40011003;
    public static final String ERR_INVALID_PASSWORD_FORMAT_MSG = "密码格式错误";
    public static final int ERR_TOKEN_EXPIRED = 40011004;
    public static final String ERR_TOKEN_EXPIRED_MSG = "TOKEN过期";
    public static final int ERR_INVALID_PARAM_FORMAT = 40011005;
    public static final String ERR_INVALID_PARAM_FORMAT_MSG = "invalid param format";
    public static final int ERR_REQUEST_METHOD = 40011006;
    public static final String ERR_REQUEST_METHOD_MSG = "wrong request method";
    public static final int ERR_REQUEST_INVALID_PARAM = 40011007;
    public static final String ERR_REQUEST_INVALID_PARAM_MSG = "invalid request params";
    public static final int ERR_WRONG_TZ = 40011008;
    public static final String ERR_WRONG_TZ_MSG = "wrong timezon format";
    public static final int ERR_INVALID_TIME_FORMAT = 40011009;
    public static final String ERR_INVALID_TIME_FORMAT_MSG = "invalid time format";
    public static final int ERR_INVALID_KEYWORD_FORMAT = 40011010;
    public static final String ERR_INVALID_KEYWORD_FORMAT_MSG = "keyword format wrong";
    public static final int ERR_INVALID_PAGE_FORMAT = 40011011;
    public static final String ERR_INVALID_PAGE_FORMAT_MSG = "page format wrong";
    public static final int ERR_NAME_EXIST = 40011012;
    public static final String ERR_NAME_EXIST_MSG = "name was exist";
    public static final int ERR_VDMP_REQUEST_FORMAT = 40011014;
    public static final String ERR_VDMP_REQUEST_FORMAT_MSG = "vdmp request error";
    public static final int ERR_FIRMWARE_UPDATE = 40011015;
    public static final String ERR_FIRMWARE_UPDATE_MSG = "firmware update fail, code %s";
    public static final int ERR_TIMER_AOUNT = 40011016;
    public static final String ERR_TIMER_AOUNT_MSG = "timer amount error";
    public static final int ERR_DEVICE_TIMER_MAX = 40411012;
    public static final int ERR_DEVICE_AWAY_MAX = 40411013;
    // 4031 权限错误
    public static final int ERR_ACCOUNT_OR_PASSWORD_WRONG = 40311001;
    public static final String ERR_ACCOUNT_OR_PASSOWRD_WRONG_MSG = "账户或密码错误";
    public static final int ERR_ACCOUNT_ALREADY_EXIST = 40311002;
    public static final String ERR_ACCOUNT_ALREADY_EXIST_MSG = "账户名已经存在";
    public static final int ERR_FILETYPE = 40311003;
    public static final String ERR_FILETYPE_MSG = "unsurported filetype";
    public static final int ERR_OLDPASSWORD_NOT_MATCH = 40311004;
    public static final String ERR_OLDPASSWORD_NOT_MATCH_MSG = "old password not match";
    public static final int ERR_USER_DONOT_OWN_DEVICE = 40311005;
    public static final String ERR_USER_DONOT_OWN_DEVICE_MSG = "device does not  belong to user";
    public static final int ERR_CONTROLLER_BUSY = 40311006;
    public static final String ERR_CONTROLLER_BUSY_MSG = "device is busy";
    public static final int ERR_TOKEN_INVALID = 40311007;
    public static final String ERR_TOKEN_INVALID_MSG = "invalid token";
    public static final int ERR_SELF_SHARE = 40311008;
    public static final String ERR_SELF_SHARE_MSG = "self share";
    // 40311009: 已经是最新的固件版本
    public static final int ERR_LATEST_FIRMWARE_VERSION = 40311009;
    public static final String ERR_LATEST_FIRMWARE_VERSION_MSG = "device has bean latest firmware version";
    public static final int ERR_DONOT_SHARE_TO_OWN = 40311010;
    public static final String ERR_DONOT_SHARE_TO_OWN_MSG = "device does not share to device owner";
    // 删除用户数据错误
    public static final int ERR_DELETE_USER_DATA = 40311011;
    public static final String ERR_DELETE_USER_DATA_MSG = "删除用户数据失败";
    // 拒绝协议错误
    public static final int ERR_REJECT_PROTOCOL = 40311012;
    public static final String ERR_REJECT_PROTOCOL_MSG = "删除用户数据失败";

    // 4041 请求资源不存在
    public static final int ERR_ACCOUNT_NOT_EXIST = 40411001;
    public static final String ERR_ACCOUNT_NOT_EXIST_MSG = "账户不存在";
    public static final int ERR_DEVICE_NOT_EXIST = 40411002;
    public static final String ERR_DEVICE_NOT_EXIST_MSG = "设备不存在";
    public static final int ERR_SHARED_USER_NOT_EXIST = 40411003;
    public static final String ERR_SHARED_USER_NOT_EXIST_MSG = "shared people not exists";
    public static final int ERR_CONTROLLER_OFFLINE = 40411004;
    public static final String ERR_CONTROLLER_OFFLINE_MSG = "device offline";
    public static final int ERR_CONTROLLER_NO_RESPONSE = 40411005;
    public static final String ERR_CONTROLLER_NO_RESPONSE_MSG = "device no response";
    public static final int ERR_TIMER_NOT_EXISTS = 40411006;
    public static final String ERR_TIMER_NOT_EXISTS_MSG = "timer not exists";
    public static final int ERR_SCENE_NOT_EXISTS = 40411007;
    public static final String ERR_SCENE_NOT_EXISTS_MSG = "scene not exists";
    public static final int ERR_URL_NOT_FOUND = 40411008;
    public static final String ERR_URL_NOT_FOUND_MSG = "4041 not found";
    public static final int ERR_INVALID_CID = 40411009;
    public static final String ERR_INVALID_CID_MSG = "cid is empty ";

    public static final int ERR_DEVICE_TIME_ZONE = 40411020;
    public static final String ERR_DEVICE_TIME_ZONE_MSG = "设备时区不相同";

    public static final int ERR_DEVICE_TYPE_NOT_EXIST = 40411021;
    public static final String ERR_DEVICE_TYPE_NOT_EXIST_MSG = "type not found";

    // 405 method not allowed
    public static final int ERR_METHOD_NOT_ALLOWED = 40511001;
    public static final String ERR_METHOD_NOT_ALLOWED_MSG = "405 method not allowed";

    // 5001 服务器端错误
    public static final int ERR_DECODE = 50011001;
    public static final String ERR_DECODE_MSG = "解码错误";
    public static final int ERR_DATABASE = 50011002;
    public static final String ERR_DATABASE_MSG = "数据库错误";
    public static final int ERR_TIME_OUT = 50011003;
    public static final String ERR_TIME_OUT_MSG = "time out";
    public static final int ERR_INTERNAL_SERVER = 50011004;
    public static final String ERR_INTERNAL_SERVER_MSG = "server internal error";
    public static final int ERR_FAILED_TO_DELETE_TIMER = 50011005;
    public static final String ERR_FAILED_TO_DELETE_TIMER_MSG = "failed to delete timer";
    public static final int ERR_OPERATION_TO_DEVICE_FAILED = 50011006;
    public static final String ERR_OPERATION_TO_DEVICE_FAILED_MSG = "operation to device failed";
    public static final int ERR_UPGRADE_FIRM = 50011007;
    public static final String ERR_UPGRADE_FIRM_MSG = "upgrade firm failed";
    public static final int ERR_TURN_SCENE_DEVICE_PARTIAL = 50011008;
    public static final String ERR_TURN_SCENE_DEVICE_PARTIAL_MSG = "turn scene device partially succeed";

    public static final int ERR_THIRDPARTY_HTTP = 50011100;
    public static final String ERR_THIRDPARTY_HTTP_MSG = "third party http error";

    // 硬件错误
    public static final int ERR_DEVICE_TURN_OFF = 50011101;
    public static final String ERR_DEVICE_TURN_OFF_MSG = "the device is turned off";


    public static final int ERR_DEVICE_SCHEDULE_NOT_EXIST = 40014001;
    public static final String ERR_DEVICE_SCHEDULE_NOT_EXIST_MSG = "schedule not found";
    public static final String ERR_DEVICE_AWAY_MAX_MSG = "away task max";
    public static final int ERR_DEVICE_TIMING = 40014002;
    public static final String ERR_DEVICE_TIMING_MSG = "timer task max";
    public static final int ERR_DEVICE_AWAY_NOT_EXIST = 40014003;
    public static final String ERR_DEVICE_AWAY_NOT_EXIST_MSG = "away not found";
    public static final int ERR_DEVICE_SCHEDULE_MAX = 40014004;
    public static final String ERR_DEVICE_SCHEDULE_MAX_MSG = "schedule task max";
    public static final int ERR_DEVICE_SCHEDULE = 40014005;
    public static final String ERR_DEVICE_SCHEDULE_MSG = "schedule not found";
    public static final int ERR_DEVICE_TIMER_NOT_EXIST = 40014006;
    public static final String ERR_DEVICE_TIMER_NOT_EXIST_MSG = "timer not found";
    public static final int ERR_DEVICE_TIMER = 40014007;
    public static final int ERR_TIMER_COUNT_MAX = 40014008;
    public static final String ERR_TIMER_COUNT_MAX_MSG = "timer count error";
}
