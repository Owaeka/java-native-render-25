package br.dev.brunovieira.authcentral.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestUtilsTest {

    @Test
    void getClientIpAddress_fromXForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        String ip = RequestUtils.getClientIpAddress(request);

        assertThat(ip).isEqualTo("192.168.1.1");
    }

    @Test
    void getClientIpAddress_multipleIps_takesFirst() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2, 10.0.0.3");

        String ip = RequestUtils.getClientIpAddress(request);

        assertThat(ip).isEqualTo("10.0.0.1");
    }

    @Test
    void getClientIpAddress_unknownSkipped() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("172.16.0.1");

        String ip = RequestUtils.getClientIpAddress(request);

        assertThat(ip).isEqualTo("172.16.0.1");
    }

    @Test
    void getClientIpAddress_fallbackRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        // All headers return null (default mock behavior)
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String ip = RequestUtils.getClientIpAddress(request);

        assertThat(ip).isEqualTo("127.0.0.1");
    }

    @Test
    void getUserAgent_present() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        String ua = RequestUtils.getUserAgent(request);

        assertThat(ua).isEqualTo("Mozilla/5.0");
    }

    @Test
    void getUserAgent_absent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn(null);

        String ua = RequestUtils.getUserAgent(request);

        assertThat(ua).isEqualTo("Unknown");
    }

    @Test
    void getTenantKey_present() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Tenant-Key")).thenReturn("my-tenant");

        String key = RequestUtils.getTenantKey(request);

        assertThat(key).isEqualTo("my-tenant");
    }

    @Test
    void getTenantKey_absent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Tenant-Key")).thenReturn(null);

        String key = RequestUtils.getTenantKey(request);

        assertThat(key).isNull();
    }
}
