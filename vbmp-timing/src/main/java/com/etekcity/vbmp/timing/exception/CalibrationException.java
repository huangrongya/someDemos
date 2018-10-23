package com.etekcity.vbmp.timing.exception;

import lombok.Data;

@Data
public class CalibrationException extends Exception {
    private int errorCode;
    private String message;

    public CalibrationException(String message, int errorCode) {
        super(message);
        this.message = message;
        this.errorCode = errorCode;
    }
}
