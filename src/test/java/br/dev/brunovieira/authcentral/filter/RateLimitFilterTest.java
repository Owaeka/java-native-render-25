package br.dev.brunovieira.authcentral.filter;

import br.dev.brunovieira.authcentral.config.RateLimitConfig;
import tools.jackson.databind.json.JsonMapper;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private LettuceBasedProxyManager<String> proxyManager;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private BucketProxy bucket;

    @Mock
    private RemoteBucketBuilder<String> remoteBucketBuilder;

    private JsonMapper jsonMapper;
    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        rateLimitConfig = new RateLimitConfig();

        RateLimitConfig.EndpointLimit loginLimit = new RateLimitConfig.EndpointLimit();
        loginLimit.setCapacity(10);
        loginLimit.setRefillTokens(10);
        loginLimit.setRefillPeriod(60);
        rateLimitConfig.setLogin(loginLimit);

        RateLimitConfig.EndpointLimit registerLimit = new RateLimitConfig.EndpointLimit();
        registerLimit.setCapacity(5);
        registerLimit.setRefillTokens(5);
        registerLimit.setRefillPeriod(60);
        rateLimitConfig.setRegister(registerLimit);
    }

    @Test
    void disabled_passesThrough() throws Exception {
        rateLimitConfig.setEnabled(false);
        RateLimitFilter filter = new RateLimitFilter(rateLimitConfig, proxyManager, jsonMapper);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(proxyManager);
    }

    @Test
    void nonRateLimitedEndpoint_passesThrough() throws Exception {
        rateLimitConfig.setEnabled(true);
        RateLimitFilter filter = new RateLimitFilter(rateLimitConfig, proxyManager, jsonMapper);

        when(request.getRequestURI()).thenReturn("/api/v1/auth/refresh");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(proxyManager);
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginEndpoint_allowed() throws Exception {
        rateLimitConfig.setEnabled(true);
        RateLimitFilter filter = new RateLimitFilter(rateLimitConfig, proxyManager, jsonMapper);

        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        when(remoteBucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginEndpoint_rateLimited() throws Exception {
        rateLimitConfig.setEnabled(true);
        RateLimitFilter filter = new RateLimitFilter(rateLimitConfig, proxyManager, jsonMapper);

        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        when(remoteBucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429);
    }

    @Test
    @SuppressWarnings("unchecked")
    void registerEndpoint_allowed() throws Exception {
        rateLimitConfig.setEnabled(true);
        RateLimitFilter filter = new RateLimitFilter(rateLimitConfig, proxyManager, jsonMapper);

        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        when(remoteBucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void proxyManagerError_passesThrough() throws Exception {
        rateLimitConfig.setEnabled(true);
        RateLimitFilter filter = new RateLimitFilter(rateLimitConfig, proxyManager, jsonMapper);

        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        when(proxyManager.builder()).thenThrow(new RuntimeException("Redis down"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
