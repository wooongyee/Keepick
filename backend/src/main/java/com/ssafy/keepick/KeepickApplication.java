package com.ssafy.keepick;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.data.jpa.repository.config.EnableJpaAuditing
@SpringBootApplication
public class KeepickApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeepickApplication.class, args);
    }
}
