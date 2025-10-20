package com.raxrot.back.configs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "🐾 RaxRot API — Petgram Platform",
                version = "1.0.0",
                description = """
                        **Petgram API** — social platform for pet lovers 🐶🐱  
                        This documentation provides all backend endpoints for authentication, posts, pets,
                        stories, analytics, donations, and more.

                        🔐 Use the “Authorize” button (top-right) to enter your JWT token:  
                        `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
                        """,
                contact = @Contact(
                        name = "RaxRot Development Team",
                        url = "https://github.com/raxrot",
                        email = "dasistperfektos@gmail.com"
                ),
                license = @License(
                        name = "Apache License 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        ),
        servers = {
                @Server(description = "Local environment", url = "http://localhost:8080"),
                @Server(description = "Production server", url = "")
        }
)
@SecurityScheme(
        name = "Bearer Authentication",
        description = "JWT-based authentication. Enter your token as: **Bearer &lt;your-token&gt;**",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {
}
