package com.etekcity.vbmp.common.comm.dto.inner;

import lombok.Data;

/**
 * googleHome,Alexa使用对象
 */
@Data
public class DeviceGoogleHome {
    /**
     * 设备cid
     */
    private String id;
    /**
     * 设备别名
     */
    private String alias;
    /**
     * 设备的在线状态 online offline  (设备上线下线)
     */
    private String status;
    /**
     * 设备开关的状态 open break (设备开或关)
     */
    private String relay;

    private String type;
    /**
     * @Description: 131 Google Home使用
     * @Author: royle.Huang
     * @Date: 2018/8/8
     */
    private String mode;
    private String speed;

}
