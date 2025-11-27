package com.tibet.tourism;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 启用定时任务功能
public class TibetTourismApplication {

    public static void main(String[] args) {
        SpringApplication.run(TibetTourismApplication.class, args);
    }

}
