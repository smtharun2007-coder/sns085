package com.authease.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ClientIpResolverTest {

    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientIpResolver();
    }

    @Test
    void testResolveClientIp_FromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
        request.setRemoteAddr("10.0.0.1");

        String clientIp = resolver.resolveClientIp(request);
        assertEquals("203.0.113.195", clientIp);
    }

    @Test
    void testResolveClientIp_FallbackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.42");

        String clientIp = resolver.resolveClientIp(request);
        assertEquals("198.51.100.42", clientIp);
    }

    @Test
    void testNetworkPrefixHash_IPv4Subnet() {
        // Two IPs in same /24 subnet (192.168.1.0/24) should yield the same network prefix hash
        String hashA = resolver.getNetworkPrefixHash("192.168.1.50");
        String hashB = resolver.getNetworkPrefixHash("192.168.1.120");
        String hashOther = resolver.getNetworkPrefixHash("192.168.2.50");

        assertNotNull(hashA);
        assertEquals(hashA, hashB);
        assertNotEquals(hashA, hashOther);
    }

    @Test
    void testNetworkPrefixHash_IPv6Subnet() {
        // Two IPs in same /48 prefix
        String ip1 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        String ip2 = "2001:0db8:85a3:ffff:0000:8a2e:0370:7335";
        String ipOther = "2001:0db8:85a4:0000:0000:8a2e:0370:7334";

        String hash1 = resolver.getNetworkPrefixHash(ip1);
        String hash2 = resolver.getNetworkPrefixHash(ip2);
        String hashOther = resolver.getNetworkPrefixHash(ipOther);

        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hashOther);
    }
}
