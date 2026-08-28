package com.hemorede;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HemoredeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HemoredeApplication.class, args);
    }
}
