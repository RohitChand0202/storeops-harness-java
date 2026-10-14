package com.cognizant.storeops.shared.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Protects every application API route while leaving actuator health probes available. */
@Configuration
public class ApiWebConfig implements WebMvcConfigurer {

    private final ApiAuthenticationInterceptor authenticationInterceptor;

    /** Registers the shared API authenticator. */
    public ApiWebConfig(ApiAuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor).addPathPatterns("/api/**");
    }
}
