package com.flakomencia.agendaflow.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary stateless API security. This configuration will be replaced by JWT security in a later phase.
 */
@Configuration
public class BootstrapSecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/system/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/health/**")
                        .permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}
