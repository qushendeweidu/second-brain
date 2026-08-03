package com.laodeng.backend.utils;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/29 22:36
 * @description ThreadLocal工具类
 */

public class ThreadLocalUtils {
    private ThreadLocalUtils() {
    }

    /**
     * 当前线程用户信息
     */
    private static final ThreadLocal<Object> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 保存用户ID
     */
    public static <T> void setData(T data) {
        THREAD_LOCAL.set(data);
    }

    /**
     * 获取用户ID
     */
    public static <T> T getData() {
        return (T) THREAD_LOCAL.get();
    }

    /**
     * 清除ThreadLocal（必须调用）
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
