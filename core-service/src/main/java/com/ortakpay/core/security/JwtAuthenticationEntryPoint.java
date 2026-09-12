package com.ortakpay.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Spring Security's filter chain runs before the DispatcherServlet, so an
 * AuthenticationException thrown there never reaches {@code GlobalExceptionHandler}
 * - this is the equivalent entry point for the security layer, kept in the same
 * RFC 7807 ProblemDetail shape as the rest of the API's errors.
 *
 * <p>Uses its own plain {@code ObjectMapper} rather than an injected bean: Spring
 * Boot 4 defaults to the new Jackson 3 ({@code tools.jackson}) stack and no longer
 * auto-configures a classic Jackson 2 {@code ObjectMapper} bean, but this class
 * only ever serializes a plain {@code ProblemDetail} (no dates, no custom modules
 * needed), so there is nothing gained by depending on Spring's Jackson setup here.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource");
        problem.setTitle("Unauthorized");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
