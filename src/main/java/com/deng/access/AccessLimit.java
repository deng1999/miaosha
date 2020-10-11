package com.deng.access;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户访问拦截的注解
 * 主要用于防止刷功能的实现
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AccessLimit {
    int seconds();//最大请求次数的时间间隔

    int maxAccessCount();//最大请求次数
    //是否要重新登陆
    boolean needLogin() default true;
}
