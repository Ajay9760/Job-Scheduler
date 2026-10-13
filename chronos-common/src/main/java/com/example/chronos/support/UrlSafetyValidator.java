package com.example.chronos.support;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * SSRF guard. Jobs call arbitrary user-supplied URLs from inside our network, so we refuse
 * non-http(s) schemes, credentials in the URL, and any host that resolves to a loopback,
 * private, link-local (cloud metadata 169.254.169.254), multicast or unspecified address.
 * It is enforced at job creation AND again right before every call (DNS can change).
 * Set chronos.security.allow-private-targets=true only for local development.
 */
public final class UrlSafetyValidator {

    private static final Set<String> SCHEMES = Set.of("http", "https");
    private final boolean allowPrivate;

    public UrlSafetyValidator(boolean allowPrivate) {
        this.allowPrivate = allowPrivate;
    }

    public void validate(String url) {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Malformed URL");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!SCHEMES.contains(scheme)) {
            throw new IllegalArgumentException("Only http and https URLs are allowed");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Credentials in URLs are not allowed");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL has no host");
        }
        if (allowPrivate) {
            return;
        }
        InetAddress[] addrs;
        try {
            addrs = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Host cannot be resolved: " + host);
        }
        for (InetAddress a : addrs) {
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                    || a.isSiteLocalAddress() || a.isMulticastAddress() || isUniqueLocalV6(a) || isCgnat(a)) {
                throw new IllegalArgumentException("URL points to a private or internal address");
            }
        }
    }

    private static boolean isUniqueLocalV6(InetAddress a) {
        byte[] b = a.getAddress();
        return b.length == 16 && (b[0] & 0xFE) == 0xFC;
    }

    private static boolean isCgnat(InetAddress a) {
        byte[] b = a.getAddress();
        return b.length == 4 && (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64;
    }
}
