package com.tibet.tourism.common.security;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OutboundUrlValidator {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "metadata.google.internal",
            "metadata",
            "169.254.169.254");

    public void validateHttpsUrl(String name, String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        URI uri = parseConfiguredUri(name, url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException(name + " must use HTTPS");
        }
        validatePublicHost(name, uri);
    }

    public void validateHttpOrHttpsServiceUrl(String name, String url, Set<String> allowedLocalHosts) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        URI uri = parseConfiguredUri(name, url);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException(name + " must use HTTP or HTTPS");
        }
        String host = requireAsciiHost(name, uri);
        if (allowedLocalHosts != null && allowedLocalHosts.contains(host.toLowerCase(Locale.ROOT))) {
            return;
        }
        rejectBlockedHost(name, host);
        rejectUnsafeResolvedAddresses(name, host);
    }

    private void validatePublicHost(String name, URI uri) {
        String host = requireAsciiHost(name, uri);
        rejectBlockedHost(name, host);
        rejectUnsafeResolvedAddresses(name, host);
    }

    private URI parseConfiguredUri(String name, String url) {
        try {
            URI uri = URI.create(url.trim());
            if (uri.getRawUserInfo() != null || uri.getHost() == null || uri.getScheme() == null) {
                throw new IllegalStateException(name + " is not a valid service URL");
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(name + " is not a valid service URL", e);
        }
    }

    private String requireAsciiHost(String name, URI uri) {
        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw new IllegalStateException(name + " must include a host");
        }
        String asciiHost = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES);
        boolean asciiOnly = host.chars().allMatch(ch -> ch < 128);
        if (!asciiOnly || !asciiHost.equals(host)) {
            throw new IllegalStateException(name + " host must be ASCII");
        }
        return host;
    }

    private void rejectBlockedHost(String name, String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if (BLOCKED_HOSTS.contains(normalized) || normalized.endsWith(".metadata.google.internal")) {
            throw new IllegalStateException(name + " host is blocked");
        }
    }

    private void rejectUnsafeResolvedAddresses(String name, String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isBlockedAddress(address)) {
                    throw new IllegalStateException(name + " resolves to a blocked network address");
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(name + " host could not be resolved safely", e);
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            return first == 0
                    || first == 10
                    || first == 127
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 169 && second == 254)
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168);
        }
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }
}
