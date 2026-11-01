package com.tibet.tourism.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TrustedProxyIpResolver {

    @Value("${app.security.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    @Value("${app.security.trusted-proxy-cidrs:}")
    private String trustedProxyCidrs;

    public String resolveClientIp(HttpServletRequest request) {
        return resolveClientIp(request, trustProxyHeaders, trustedProxyCidrs);
    }

    /**
     * Whether the request arrived from a configured reverse proxy, meaning its {@code X-Forwarded-*}
     * headers may be believed. Callers that reconstruct the externally visible scheme or host need
     * this: behind a TLS-terminating proxy the connector itself only ever sees plain HTTP.
     */
    public boolean isTrustedProxyPeer(HttpServletRequest request) {
        return trustProxyHeaders && isTrustedProxy(fallbackRemoteAddr(request), trustedProxyCidrs);
    }

    public String resolveClientIp(HttpServletRequest request, boolean trustProxyHeaders, String trustedProxyCidrs) {
        String remoteAddr = fallbackRemoteAddr(request);
        if (!trustProxyHeaders || !isTrustedProxy(remoteAddr, trustedProxyCidrs)) {
            return remoteAddr;
        }

        String forwardedFor = firstUntrustedForwardedFor(request.getHeader("X-Forwarded-For"), trustedProxyCidrs);
        if (hasUsableIp(forwardedFor)) {
            return forwardedFor;
        }

        String realIp = normalizeIp(request.getHeader("X-Real-IP"));
        return isValidIp(realIp) ? realIp : remoteAddr;
    }

    private String firstUntrustedForwardedFor(String headerValue, String trustedProxyCidrs) {
        if (!StringUtils.hasText(headerValue) || headerValue.length() > 512) {
            return "";
        }

        List<String> candidates = new ArrayList<>();
        for (String part : headerValue.split(",")) {
            String candidate = normalizeIp(part);
            if (isValidIp(candidate)) {
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            return "";
        }

        for (int i = candidates.size() - 1; i >= 0; i--) {
            String candidate = candidates.get(i);
            if (!isTrustedProxy(candidate, trustedProxyCidrs)) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private String fallbackRemoteAddr(HttpServletRequest request) {
        String remoteAddr = normalizeIp(request.getRemoteAddr());
        return isValidIp(remoteAddr) ? remoteAddr : "unknown";
    }

    private boolean isTrustedProxy(String ipAddress, String trustedProxyCidrs) {
        if (!StringUtils.hasText(trustedProxyCidrs)) {
            return false;
        }
        InetAddress address = parseInetAddress(ipAddress);
        if (address == null) {
            return false;
        }
        for (String rawCidr : trustedProxyCidrs.split(",")) {
            TrustedCidr cidr = parseTrustedCidr(rawCidr);
            if (cidr != null && cidr.contains(address)) {
                return true;
            }
        }
        return false;
    }

    private TrustedCidr parseTrustedCidr(String rawCidr) {
        if (!StringUtils.hasText(rawCidr)) {
            return null;
        }
        String cidr = rawCidr.trim();
        String addressPart = cidr;
        Integer prefixLength = null;
        int slash = cidr.indexOf('/');
        if (slash >= 0) {
            addressPart = cidr.substring(0, slash);
            try {
                prefixLength = Integer.parseInt(cidr.substring(slash + 1));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        InetAddress network = parseInetAddress(addressPart);
        if (network == null) {
            return null;
        }
        int maxBits = network.getAddress().length * 8;
        int actualPrefix = prefixLength == null ? maxBits : prefixLength;
        if (actualPrefix < 0 || actualPrefix > maxBits) {
            return null;
        }
        return new TrustedCidr(network.getAddress(), actualPrefix);
    }

    private InetAddress parseInetAddress(String value) {
        String normalized = normalizeIp(value);
        if (!hasUsableIp(normalized)) {
            return null;
        }
        try {
            if (normalized.contains(":")) {
                String withoutZone = normalized.split("%", 2)[0];
                if (!withoutZone.matches("[0-9A-Fa-f:.]+")) {
                    return null;
                }
                return InetAddress.getByName(withoutZone);
            }
            if (!isIpv4Literal(normalized)) {
                return null;
            }
            return InetAddress.getByName(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeIp(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() > 128 || "unknown".equalsIgnoreCase(normalized)) {
            return "";
        }
        if (normalized.startsWith("[")) {
            int endBracket = normalized.indexOf(']');
            if (endBracket > 0) {
                normalized = normalized.substring(1, endBracket);
            }
        } else if (isIpv4WithPort(normalized)) {
            normalized = normalized.substring(0, normalized.indexOf(':'));
        }
        if ("0:0:0:0:0:0:0:1".equals(normalized) || "::1".equals(normalized)) {
            return "127.0.0.1";
        }
        return normalized;
    }

    private boolean isIpv4WithPort(String value) {
        int colon = value.indexOf(':');
        return colon > 0 && colon == value.lastIndexOf(':') && value.substring(0, colon).contains(".");
    }

    private boolean isIpv4Literal(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            try {
                int parsed = Integer.parseInt(part);
                if (parsed < 0 || parsed > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private boolean hasUsableIp(String value) {
        return StringUtils.hasText(value) && value.length() <= 128 && !"unknown".equalsIgnoreCase(value.trim());
    }

    private boolean isValidIp(String value) {
        return parseInetAddress(value) != null;
    }

    private record TrustedCidr(byte[] networkAddress, int prefixLength) {
        boolean contains(InetAddress address) {
            byte[] candidate = address.getAddress();
            if (candidate.length != networkAddress.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (candidate[i] != networkAddress[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            return ((candidate[fullBytes] & 0xFF) & mask) == ((networkAddress[fullBytes] & 0xFF) & mask);
        }
    }
}
