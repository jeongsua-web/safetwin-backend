package com.safetwin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SafeTwinApplication {

    public static void main(String[] args) {
        SpringApplication.run(SafeTwinApplication.class, args);
    }
}
