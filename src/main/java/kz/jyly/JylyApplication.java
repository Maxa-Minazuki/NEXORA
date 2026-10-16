package kz.jyly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JylyApplication {

    public static void main(String[] args) {
        SpringApplication.run(JylyApplication.class, args);
    }
}
