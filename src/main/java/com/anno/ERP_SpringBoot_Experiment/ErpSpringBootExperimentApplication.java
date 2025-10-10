package com.anno.ERP_SpringBoot_Experiment;

import com.anno.ERP_SpringBoot_Experiment.config.MinioProperties;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@EnableConfigurationProperties({SecurityProperties.class, MinioProperties.class, CacheProperties.class})
public class ErpSpringBootExperimentApplication {

    private static final Logger log = LoggerFactory.getLogger(ErpSpringBootExperimentApplication.class);

    public static void main(String[] args) {
        final var app = new SpringApplication(ErpSpringBootExperimentApplication.class);
        final var env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(final Environment env) {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        String serverPort = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path");
        if (StringUtils.isBlank(contextPath)) {
            contextPath = "/";
        }
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }

        String swaggerUiPath = env.getProperty("springdoc.swagger-ui.path");
        if (StringUtils.isBlank(swaggerUiPath)) {
            swaggerUiPath = "/swagger-ui.html";
        }

        log.info("""

                        ----------------------------------------------------------
                        \tApplication '{}' is running! Access URLs:
                        \tLocal: \t\t{}://localhost:{}{}
                        \tSwagger UI: \t{}://localhost:{}{}{}
                        \tExternal: \t{}://{}:{}{}
                        \tProfile(s): \t{}
                        ----------------------------------------------------------""",
                env.getProperty("spring.application.name"),
                protocol,
                serverPort,
                contextPath,
                protocol,
                serverPort,
                contextPath.equals("/") ? "" : contextPath,
                swaggerUiPath,
                protocol,
                hostAddress,
                serverPort,
                contextPath,
                env.getActiveProfiles());
    }

}
