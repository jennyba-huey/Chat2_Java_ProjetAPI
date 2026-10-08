package com.example.messagerie.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation Swagger / OpenAPI de l'API REST.
 * Page : http://localhost:8080/swagger-ui.html
 * Le bouton "Authorize" permet de coller le jeton JWT recu au login :
 * Swagger l'envoie ensuite dans l'en-tete Authorization: Bearer <jeton>.
 * Le canal WebSocket (/ws/messages) n'apparait pas ici : Swagger ne documente que le REST.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API Messagerie instantanée",
                version = "1.0",
                description = "Comptes, authentification JWT, liste des utilisateurs et historique des messages."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}