package com.aurxsiu.bupthelper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.aurxsiu.bupthelper.config.JwtProperties;
import com.aurxsiu.bupthelper.file.UploadProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, UploadProperties.class})
public class BuptHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuptHelperApplication.class, args);
    }

}
