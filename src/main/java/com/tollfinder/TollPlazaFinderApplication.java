package com.tollfinder;

import com.tollfinder.config.FinderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FinderProperties.class)
public class TollPlazaFinderApplication {
    public static void main(String[] args) {
        SpringApplication.run(TollPlazaFinderApplication.class, args);
    }
}
