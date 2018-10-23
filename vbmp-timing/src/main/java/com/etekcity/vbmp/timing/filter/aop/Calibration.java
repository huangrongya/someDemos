package com.etekcity.vbmp.timing.filter.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验
 * 用于校验方法参数（参数必须为json或者json格式的字符串,且只验证第一个参数）
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Calibration {
    /**
     * 需要校验的字段，不需校验时不写
     *
     * @return
     */
    String[] fields() default {};

    /**
     * 是否需要校验token
     *
     * @return
     */
    boolean checkToken() default false;

    /**
     * 是否需要校验设备存在（使用uuid校验）
     *
     * @return
     */
    boolean checkDevice() default false;

    /**
     * 是否需要校验设备在线
     *
     * @return
     */
    boolean checkOnline() default false;
}
