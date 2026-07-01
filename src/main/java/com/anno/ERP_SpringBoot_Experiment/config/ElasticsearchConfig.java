package com.anno.ERP_SpringBoot_Experiment.config;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestClientBuilderCustomizer restClientBuilderCustomizer(ElasticsearchProperties properties) {
        return builder -> builder.setHttpClientConfigCallback(httpClientBuilder -> {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }}, new SecureRandom());

                httpClientBuilder
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier((hostname, session) -> true);

                if (properties.getUsername() != null && properties.getPassword() != null) {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(
                            AuthScope.ANY,
                            new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword())
                    );
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }

                return httpClientBuilder;
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure Elasticsearch SSL trust-all context", e);
            }
        });
    }
}
