package com.anno.ERP_SpringBoot_Experiment.fineract.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "fineract")
public class FineractProperties {
    private String baseUrl = "https://localhost:8443/fineract-provider/api/v1";
    private String tenantId = "default";
    private String username = "mifos";
    private String password = "password";
}
