package br.dev.brunovieira.authcentral.filter;

import br.dev.brunovieira.authcentral.config.RateLimitConfig;
import br.dev.brunovieira.authcentral.dto.response.ErrorResponse;
import br.dev.brunovieira.authcentral.util.RequestUtils;
import tools.jackson.databind.json.JsonMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final LettuceBasedProxyManager<String> proxyManager;
    private final JsonMapper jsonMapper;
    private final Map<String, Supplier<BucketConfiguration>> configCache;

    public RateLimitFilter(
            RateLimitConfig rateLimitConfig,
            LettuceBasedProxyManager<String> proxyManager,
            JsonMapper jsonMapper
    ) {
        this.rateLimitConfig = rateLimitConfig;
        this.proxyManager = proxyManager;
        this.jsonMapper = jsonMapper;
        this.configCache = buildConfigCache(rateLimitConfig);
    }

    private static Map<String, Supplier<BucketConfiguration>> buildConfigCache(RateLimitConfig config) {
        return Map.of(
                "login", toBucketConfigSupplier(config.getLogin()),
                "register", toBucketConfigSupplier(config.getRegister())
        );
    }

    private static Supplier<BucketConfiguration> toBucketConfigSupplier(RateLimitConfig.EndpointLimit limit) {
        if (limit == null) {
            return null;
        }
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(
                        limit.getCapacity(),
                        Refill.intervally(limit.getRefillTokens(), Duration.ofSeconds(limit.getRefillPeriod()))
                ))
                .build();
        return () -> configuration;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!rateLimitConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestURI = request.getRequestURI();
        String endpointKey = getEndpointKey(requestURI);

        if (endpointKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Supplier<BucketConfiguration> configSupplier = configCache.get(endpointKey);
        if (configSupplier == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = RequestUtils.getClientIpAddress(request);
        String bucketKey = "rate_limit:" + requestURI + ":" + clientIp;

        try {
            Bucket bucket = proxyManager.builder().build(bucketKey, configSupplier);

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                log.warn("Rate limit exceeded for IP {} on endpoint {}", clientIp, requestURI);
                sendRateLimitError(response);
            }
        } catch (Exception e) {
            log.error("Error checking rate limit, allowing request: {}", e.getMessage());
            filterChain.doFilter(request, response);
        }
    }

    private String getEndpointKey(String requestURI) {
        if (requestURI.endsWith("/login")) {
            return "login";
        } else if (requestURI.endsWith("/register")) {
            return "register";
        }
        return null;
    }

    private void sendRateLimitError(HttpServletResponse response) throws IOException {
        ErrorResponse error = ErrorResponse.builder()
                .status("error")
                .message("Rate limit exceeded. Please try again later.")
                .code(HttpStatus.TOO_MANY_REQUESTS.value())
                .details("Too many requests. Please try again later.")
                .timestamp(LocalDateTime.now())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(jsonMapper.writeValueAsString(error));
        response.getWriter().flush();
    }
}
