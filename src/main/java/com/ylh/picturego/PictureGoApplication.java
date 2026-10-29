package com.ylh.picturego;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.ylh.picturego.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
public class PictureGoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureGoApplication.class, args);
    }

}
