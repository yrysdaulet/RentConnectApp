package com.rentconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class RentConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentConnectApplication.class, args);
    }
}

