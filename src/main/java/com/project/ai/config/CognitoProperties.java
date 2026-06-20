package com.project.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 7:39 PM
 */
@ConfigurationProperties(prefix = "aws.cognito")
@Getter
@Setter
public class CognitoProperties {

    private String region;
    private String userPoolId;
    private String clientId;
    private String clientSecret;
    private String issuerUri;
}
