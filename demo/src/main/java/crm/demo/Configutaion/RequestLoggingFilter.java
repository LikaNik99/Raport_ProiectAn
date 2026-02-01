package crm.demo.Configutaion;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String ip = GetClientIP.getClientIP(httpRequest);
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        System.out.printf(
                "Request %s %s from %s%n",
                method,
                uri,
                ip
        );

        chain.doFilter(request, response);
    }
}
