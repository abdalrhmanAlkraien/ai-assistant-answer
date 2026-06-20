package com.project.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 9:20 PM
 */
@Configuration
@EnableConfigurationProperties(CognitoProperties.class)
public class CognitoConfig {

    private final CognitoProperties props;

    public CognitoConfig(CognitoProperties props) {
        this.props = props;
    }

    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(ProfileCredentialsProvider.create("migfora-rag_dev"))
                .build();
    }
}
