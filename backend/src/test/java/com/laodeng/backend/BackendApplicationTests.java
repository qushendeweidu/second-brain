package com.laodeng.backend;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;

@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoads() {
        SecretKey key = Jwts.SIG.HS512.key().build();
        System.out.println(Encoders.BASE64.encode(key.getEncoded()));
    }

}
