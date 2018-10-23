package com.etekcity.vbmp.device.control.constant;

public class ErrorConstant {
    //400错误
    public static final int ERR_REQUEST_PARAM_EMPTY = 4001001;
    public static final String ERR_REQUEST_PARAM_EMPTY_MSG = "参数为空";

    public static final int ERR_REQUEST_NOT_AGREEMENT = 4001002;
    public static final String ERR_REQUEST_NOT_AGREEMENT_MSG = "通用协议为null";

    public static final int ERR_REQUEST_INVALID_PARAM = 4001007;
    public static final String ERR_REQUEST_INVALID_PARAM_MSG = "数字格式化异常";

    public static final int ERR_REQUEST_INVALID_METHOD = 4001009;
    public static final String ERR_REQUEST_INVALID_METHOD_MSG = "不支持的请求方法";

    //500错误
    public static final int ERR_DATABASE = 5001002;
    public static final String ERR_DATABASE_MSG = "数据库错误";
}
