package com.statemachine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stateMachineOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("State Machine API")
                .description("REST API for managing state machine instances and definitions. " +
                    "This API allows you to create machine instances, execute transitions, " +
                    "update context, and retrieve machine history.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("State Machine API Support")
                    .email("support@statemachine.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
