package io.github.eendroroy.fuelquota.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web MVC configuration to handle SPA (Single Page Application) routing.
 * <p>
 * Serves index.html for all non-API routes that don't match static resources.
 * This allows frontend React Router to handle client-side routing.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // Don't interfere with API routes
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // For SPA routes, return index.html so React Router can handle routing
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}


