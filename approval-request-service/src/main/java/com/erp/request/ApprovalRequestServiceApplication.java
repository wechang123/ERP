package com.erp.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApprovalRequestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApprovalRequestServiceApplication.class, args);
    }
}
