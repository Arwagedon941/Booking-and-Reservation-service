package com.example.platform.servicetwo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ServiceTwoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceTwoApplication.class, args);
    }
}

