package com.aurxsiu.bupthelper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.aurxsiu.bupthelper.config.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class BuptHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuptHelperApplication.class, args);
    }

}
