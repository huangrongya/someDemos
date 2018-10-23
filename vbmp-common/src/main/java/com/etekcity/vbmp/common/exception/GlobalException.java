package com.etekcity.vbmp.common.exception;

import com.etekcity.vbmp.common.config.VBMPResponse;
import com.etekcity.vbmp.common.constant.ErrorConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
@Slf4j
public class GlobalException {

    @ExceptionHandler(ServiceException.class)
    public VBMPResponse handlerBusinessException(HttpServletRequest request, ServiceException e) {
        handleLog(request, e);
        return new VBMPResponse(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(CalibrationException.class)
    public VBMPResponse handlerCalibrationException(HttpServletRequest request, CalibrationException e) {
        handleLog(request, e);
        return new VBMPResponse(e.getCode(), e.getMessage());
    }

    // 放在最后面，catch其它异常
    @ExceptionHandler(Exception.class)
    public VBMPResponse handlerException(HttpServletRequest request, Exception e) {
        handleLog(request, e);
        return new VBMPResponse(ErrorConstant.ERR_INTERNAL_SERVER, ErrorConstant.ERR_INTERNAL_SERVER_MSG);
    }

    private void handleLog(HttpServletRequest request, Exception e) {
        StringBuffer sb = new StringBuffer();
        if (request != null) {
            sb.append("request method:").append(request.getMethod()).append(", ").append("url:")
                    .append(request.getRequestURL()).append(", ");
        }
        sb.append("exception:").append(e);
        log.error(sb.toString(), e);
    }
}
