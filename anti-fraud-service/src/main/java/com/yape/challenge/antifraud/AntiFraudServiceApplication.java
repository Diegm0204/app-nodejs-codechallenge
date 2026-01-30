package com.yape.challenge.antifraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;


@SpringBootApplication(scanBasePackages = "com.yape.challenge")
@EnableKafka
public class AntiFraudServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AntiFraudServiceApplication.class, args);
    }
}
