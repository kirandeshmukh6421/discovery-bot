package com.discoverybot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class DiscoveryBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryBotApplication.class, args);
    }
}
