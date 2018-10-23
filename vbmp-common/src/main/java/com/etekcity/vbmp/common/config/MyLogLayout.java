package com.etekcity.vbmp.common.config;

import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;

import java.sql.Timestamp;

/**
 * @Description: 日志信息format
 * @Author: royle.Huang
 * @Date: 2018/9/12
 */
public class MyLogLayout extends LayoutBase<ILoggingEvent> {

    private static final String PROJECT_NAME = "vbmp-common";

    @Override
    public String doLayout(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder();
        String message = event.getFormattedMessage();
        if (message.startsWith("{") && message.endsWith("}\n")){
            sb.append(event.getFormattedMessage());
        }else {
            sb.append("{");
            sb.append("\"P\":");
            sb.append("\"" + PROJECT_NAME + "\", ");
            sb.append("\"T\":");
            sb.append("\"").append(new Timestamp(event.getTimeStamp())).append("\"");
            sb.append(", \"Timestamp\":");
            sb.append("\"").append(event.getTimeStamp()).append("\"");
            sb.append(", \"L\":");
            sb.append("\"").append(event.getLevel()).append("\"");
            sb.append(", \"CLASS\": ");
            if (event.getCallerData().length > 0) {
                StackTraceElement callerData = event.getCallerData()[0];
                int lineNumber = callerData.getLineNumber();
                String method = callerData.getMethodName();
                sb.append("\"").append(event.getLoggerName()).append(".").append(method).append(": ").append(lineNumber).append("\"");
            } else {
                sb.append("\"").append(event.getLoggerName()).append("\"");
            }

            if (event.getThrowableProxy() != null) {
                ExtendedThrowableProxyConverter throwableConverter = new ExtendedThrowableProxyConverter();
                throwableConverter.start();
                message = event.getFormattedMessage() + " | " + throwableConverter.convert(event);
                throwableConverter.stop();
            }
            sb.append(",\"M\": ");
            sb.append("\"").append(message).append("\"");
            sb.append("}");
            sb.append(CoreConstants.LINE_SEPARATOR);
        }
        return sb.toString();
    }
}
