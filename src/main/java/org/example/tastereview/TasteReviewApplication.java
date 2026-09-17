package org.example.tastereview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class TasteReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(TasteReviewApplication.class, args);
    }

}
