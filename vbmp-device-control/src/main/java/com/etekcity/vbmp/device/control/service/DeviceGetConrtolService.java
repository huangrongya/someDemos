package com.etekcity.vbmp.device.control.service;

import com.etekcity.vbmp.device.control.dto.ErrorResponse;
import com.etekcity.vbmp.device.control.dto.GetConrtolRequest;
import com.etekcity.vbmp.device.control.dto.GetConrtolResponse;
import com.etekcity.vbmp.device.control.dto.InsertConrtolRequest;

public interface DeviceGetConrtolService {
    /**
     * 协议查询并发送至vdmp
     */

    GetConrtolResponse getConrtolService(GetConrtolRequest request) throws Exception;

    ErrorResponse insetConrtolService(InsertConrtolRequest request) throws Exception;
}
