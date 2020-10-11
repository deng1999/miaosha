package com.deng.until;

import java.util.UUID;

/**
 * UUID工具类用于生成session
 * 直接生成的ID中有“-”存在，如果不需要，可以使用replace()方法去掉。
 */
public class UUIDUtil {
    public static String uuid(){
        return UUID.randomUUID().toString().replace("-","");
    }
}
