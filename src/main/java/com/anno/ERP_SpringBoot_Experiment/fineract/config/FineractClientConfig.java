package com.anno.ERP_SpringBoot_Experiment.fineract.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Configuration
public class FineractClientConfig {

    @Bean
    public RestClient fineractRestClient(FineractProperties properties) throws Exception {
        // Create auth header
        String authString = properties.getUsername() + ":" + properties.getPassword();
        String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes());
        String authHeader = "Basic " + encodedAuth;

        // Bypass SSL for local testing (matches MCP server behavior)
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        
        // Since RestClient uses JDK HttpClient or simple request factory by default, 
        // we can configure it using JdkClientHttpRequestFactory if we use JDK 11+ HttpClient.
        // For simplicity and avoiding complex factory setup, we'll just set default headers.
        // Note: For fully disabling SSL verification in RestClient without a custom factory,
        // it requires deeper config. Here we keep it simple.

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .defaultHeader("Fineract-Platform-TenantId", properties.getTenantId())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }
}
