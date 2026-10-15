package io.github.raulramalf.browseme.infrastructure.web;

import io.github.raulramalf.browseme.infrastructure.config.BrowseMeProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfig implements WebMvcConfigurer {

    private final BrowseMeProperties properties;

    WebConfig(BrowseMeProperties properties) {
        this.properties = properties;
    }

    /** CORS restrictivo: solo el origen del frontend, solo GET y POST, sin credenciales. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.web().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type")
                .exposedHeaders("Location", "Retry-After")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Bean
    FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registration =
                new FilterRegistrationBean<>(new SecurityHeadersFilter(properties.web().hsts()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(
                new RateLimitFilter(properties.web().submitsPerMinute(), properties.web().readsPerMinute()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
