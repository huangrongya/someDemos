package com.etekcity.vbmp.common.comm.dto;

import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.comm.dto.inner.DeviceGoogleHome;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DevicesGoogleHomeResponse extends VBMPResponse {
    // private List<Device> devices;
    private List<DeviceGoogleHome> deviceGoogleHomes;
}
