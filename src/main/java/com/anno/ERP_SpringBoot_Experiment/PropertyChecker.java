package com.anno.ERP_SpringBoot_Experiment;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct; // Hoặc javax.annotation.PostConstruct cho Spring Boot < 3

@Component
public class PropertyChecker {

    private final Environment env;

    public PropertyChecker(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void checkProperty() {
        String secretKey = env.getProperty("application.security.jwt.secret-key");
        if (secretKey == null) {
            System.err.println("ERROR: application.security.jwt.secret-key is NOT FOUND in Environment");
        } else {
            System.out.println("INFO: application.security.jwt.secret-key found: " + secretKey);
        }
    }
}
    