package com.pr1.newletsassemble;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NewLetsAssembleApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewLetsAssembleApplication.class, args);
    }

}
