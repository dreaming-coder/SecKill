package com.example.repository.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author ice
 * @create 2021-05-27 09:18:38
 */
@Configuration
public class DruidConfiguration {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource") // 和配置文件中的属性绑定，避免重复写 setXXX()
    public DataSource dataSource(){
        return new DruidDataSource();
    }
}
