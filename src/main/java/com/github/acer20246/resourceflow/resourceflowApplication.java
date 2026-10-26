package com.github.acer20246.resourceflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class resourceflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(resourceflowApplication.class, args);
    }

}
