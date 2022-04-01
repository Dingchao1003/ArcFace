package com.vishee.arcface;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.vishee.arcface.mapper")
public class ArcfaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArcfaceApplication.class, args);
    }

}
