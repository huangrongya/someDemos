package com.etekcity.vbmp.timing.common.controller;

import com.alibaba.fastjson.JSON;
import com.etekcity.vbmp.timing.common.VBMPResponse;
import com.etekcity.vbmp.timing.common.service.CommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vbmp/timing/common")
public class CommonController {

    @Autowired
    CommonService commonService;

    @PostMapping("/updateUuid")
    public VBMPResponse updateUuid(@RequestBody String req) {
        String oldUuid = JSON.parseObject(req).getString("oldUuid");
        String newUuid = JSON.parseObject(req).getString("newUuid");
        return commonService.updateUuidByUuid(oldUuid, newUuid);
    }
    @PostMapping("/deleteByUuid")
    public VBMPResponse deleteByUuid(@RequestBody String req) {
        String uuid = JSON.parseObject(req).getString("uuid");
        return commonService.deleteByUuid(uuid);
    }
}
