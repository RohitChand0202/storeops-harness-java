package com.cognizant.storeops.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.cognizant.storeops.shared.error.UnauthorizedException;
import com.cognizant.storeops.staff.service.StaffService;

/** Resolves an opaque bearer token before an API controller runs. */
@Component
public class ApiAuthenticationInterceptor implements HandlerInterceptor {

    /** Request attribute consumed by authenticated controller methods. */
    public static final String PRINCIPAL_ATTRIBUTE = "storeops.authenticatedUser";

    private final StaffService staffService;

    /** Creates an interceptor backed by staff token lookup. */
    public ApiAuthenticationInterceptor(StaffService staffService) {
        this.staffService = staffService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("UNAUTHORIZED", "A bearer token is required");
        }
        String token = authorization.substring("Bearer ".length());
        if (token.isBlank() || !token.equals(token.trim())) {
            throw new UnauthorizedException("UNAUTHORIZED", "A bearer token is required");
        }
        request.setAttribute(PRINCIPAL_ATTRIBUTE, staffService.resolveToken(token));
        return true;
    }
}
