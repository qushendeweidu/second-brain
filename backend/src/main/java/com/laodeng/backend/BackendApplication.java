package com.laodeng.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/28 15:06
 * @description 启动类
 */

@EnableTransactionManagement
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan(value = "com.laodeng.backend.mapper")
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
