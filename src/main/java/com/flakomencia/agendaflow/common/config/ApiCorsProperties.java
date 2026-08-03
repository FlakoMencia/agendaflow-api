package com.flakomencia.agendaflow.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record ApiCorsProperties(List<String> allowedOrigins) {
}
