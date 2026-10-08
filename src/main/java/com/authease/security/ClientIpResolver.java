package com.authease.security;

import com.authease.util.CryptoUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
public class ClientIpResolver {

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        
        // Spring's framework strategy populates getRemoteAddr from X-Forwarded-For when configured,
        // but we inspect X-Forwarded-For as an extra defensive fallback.
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] parts = xForwardedFor.split(",");
            String clientIp = parts[0].trim();
            if (isValidIp(clientIp)) {
                return clientIp;
            }
        }
        
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "127.0.0.1";
    }

    public String getNetworkPrefixHash(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return CryptoUtil.sha256("127.0.0.0/24");
        }
        try {
            InetAddress inet = InetAddress.getByName(ipAddress.trim());
            byte[] bytes = inet.getAddress();
            if (bytes.length == 4) {
                // IPv4: /24 prefix (first 3 octets)
                String prefix = String.format("%d.%d.%d.0/24",
                        bytes[0] & 0xFF, bytes[1] & 0xFF, bytes[2] & 0xFF);
                return CryptoUtil.sha256(prefix);
            } else if (bytes.length == 16) {
                // IPv6: /48 prefix (first 6 bytes / 3 groups)
                String prefix = String.format("%02x%02x:%02x%02x:%02x%02x::/48",
                        bytes[0] & 0xFF, bytes[1] & 0xFF,
                        bytes[2] & 0xFF, bytes[3] & 0xFF,
                        bytes[4] & 0xFF, bytes[5] & 0xFF);
                return CryptoUtil.sha256(prefix);
            }
        } catch (Exception ignored) {}
        return CryptoUtil.sha256(ipAddress + "/unknown");
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) return false;
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
