package com.flakomencia.agendaflow.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(NotificationIntegrationProperties.class)
public class NotificationOutboxConfiguration {
    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder notificationRestClientBuilder() {
        return RestClient.builder();
    }
}
