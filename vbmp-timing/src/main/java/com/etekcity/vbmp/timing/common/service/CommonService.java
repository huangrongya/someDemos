package com.etekcity.vbmp.timing.common.service;

import com.etekcity.vbmp.timing.common.VBMPResponse;

public interface CommonService {
    VBMPResponse updateUuidByUuid(String oldUuid, String newUuid);

    VBMPResponse deleteByUuid(String uuid);
}
