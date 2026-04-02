package io.github.eendroroy.fuelquota.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc / OpenAPI 3 configuration for the Automated Fuel Quota System.
 *
 * <p>Exposes interactive API documentation at:
 * <ul>
 *   <li>Swagger UI: {@code /swagger-ui.html} or {@code /swagger-ui/index.html}</li>
 *   <li>OpenAPI JSON: {@code /v3/api-docs}</li>
 *   <li>OpenAPI YAML: {@code /v3/api-docs.yaml}</li>
 * </ul>
 *
 * <p>A Bearer JWT security scheme named {@value #SECURITY_SCHEME_NAME} is registered
 * globally. Protected endpoints should reference it via
 * {@code @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "Automated Fuel Quota System API",
                version     = "1.0.0",
                description = """
                        REST API for the Automated Fuel Quota Management System.

                        ## Endpoint groups
                        | Tag | Base path | Auth required |
                        |-----|-----------|---------------|
                        | Authentication | `/api/auth/**` | None |
                        | Customer | `/api/customer/**` | JWT (CUSTOMER role) |
                        | Admin | `/api/admin/**` | JWT (ADMIN role) |
                        | Pump | `/api/pump/**` | None (BRD public pump API) |

                        ## Authentication
                        1. Obtain a JWT token via `POST /api/auth/customer/login` or `POST /api/auth/admin/login`.
                        2. Click **Authorize** above and enter `Bearer <token>`.
                        3. All subsequent calls will include the token automatically.
                        """,
                contact = @Contact(
                        name  = "RedDot Digital IT",
                        url   = "https://reddotdigitalit.com",
                        email = "support@reddotdigitalit.com"
                ),
                license = @License(
                        name = "Proprietary",
                        url  = "https://reddotdigitalit.com/license"
                )
        ),
        servers = {
                @Server(url = "/",           description = "Current server"),
                @Server(url = "http://localhost:8080", description = "Local development server")
        }
)
@SecurityScheme(
        name        = OpenApiConfig.SECURITY_SCHEME_NAME,
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT",
        in          = SecuritySchemeIn.HEADER,
        description = "JWT access token obtained from the authentication endpoints. "
                    + "Format: `Bearer <token>`"
)
public class OpenApiConfig {

    /**
     * Name of the global Bearer JWT security scheme.
     * Reference this constant in {@code @SecurityRequirement} annotations on controllers.
     */
    public static final String SECURITY_SCHEME_NAME = "bearerAuth";
}

