package com.project.ai;

import com.project.ai.config.CognitoProperties;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.config.WhisperProperties;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({LangChain4jProperties.class, WhisperProperties.class, CognitoProperties.class})
@Log4j2
public class AiApplication {

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(AiApplication.class);
        app.addListeners((ApplicationReadyEvent event) -> {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("  NexAi Application started successfully");
            log.info("  Port    : {}", event.getApplicationContext()
                    .getEnvironment().getProperty("server.port", "8080"));
            log.info("  Profile : {}", String.join(", ", event.getApplicationContext()
                    .getEnvironment().getActiveProfiles()));
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        });
        app.run(args);
    }

}
