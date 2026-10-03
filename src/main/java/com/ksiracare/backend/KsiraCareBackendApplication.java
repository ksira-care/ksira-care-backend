package com.ksiracare.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KsiraCareBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KsiraCareBackendApplication.class, args);
    }

}
