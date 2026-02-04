package com.Match_Service.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate Configuration for Match Service.
 *
 * Match Service needs RestTemplate to make HTTP calls to User Service
 * (through the API Gateway) to validate that users exist before storing
 * their emails in Match entities.
 *
 * Without this @Bean, you'll get:
 *   "Parameter 2 of constructor in MatchService required a bean
 *    of type 'RestTemplate' that could not be found."
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}