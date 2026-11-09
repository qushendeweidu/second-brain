package com.laodeng.backend.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/11/6 21:36
 * @description token加工工具类
 */
public class TokenDigestUtils {


    private TokenDigestUtils() {
    }

    public static String sha256(String token) {
        try {
            // 从工厂方法中创建实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 根据UTF-8的字符集转化成字符数组
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            // 将二进制的字符数组转化成十六进制字符
            return HexFormat.of().formatHex(hash);
            //若是当前实例不存在则捕获异常
        } catch (NoSuchAlgorithmException e) {
            // 因为当前的这个实例是
            throw new IllegalStateException(e);
        }
    }
}
