package com.example.sazu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SazuApplication {

    public static void main(String[] args) {
        SpringApplication.run(SazuApplication.class, args);
    }

}
