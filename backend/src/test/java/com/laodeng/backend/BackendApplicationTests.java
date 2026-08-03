package com.laodeng.backend;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/28 15:06
 * @description 测试类
 */

@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoads() {
        SecretKey key = Jwts.SIG.HS512.key().build();
        System.out.println(Encoders.BASE64.encode(key.getEncoded()));
    }

}
