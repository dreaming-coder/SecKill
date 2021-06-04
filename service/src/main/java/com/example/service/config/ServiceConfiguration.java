package com.example.service.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 21:38:15
 */
@Configuration
public class ServiceConfiguration {

    @Bean
    @SuppressWarnings("UnstableApiUsage")
    public RateLimiter rateLimiter(){
        return RateLimiter.create(10);
    }
}
