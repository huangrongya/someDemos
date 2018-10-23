package com.etekcity.vbmp.timing.modules.away.service;

import com.etekcity.vbmp.timing.modules.away.bean.AwayRequest;
import com.etekcity.vbmp.timing.modules.away.bean.AwayResponse;

import java.text.ParseException;

public interface AwayService {
    AwayResponse addAway(AwayRequest request) throws ParseException;

    AwayResponse deleteAway(AwayRequest request);

    AwayResponse updateAway(AwayRequest request);

    AwayResponse updateAwayStatus(AwayRequest request);


    AwayResponse getAways(AwayRequest request) throws ParseException;

    AwayResponse stopAwayByPrimaryKey(Integer awayId);

    AwayResponse updateUuidByUuid(String oldUuid, String newUuid);

    AwayResponse deleteTimerByUuid(String uuid);
}
