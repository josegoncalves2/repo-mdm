package com.hmdm.plugins.webfilter.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>Normalization and validation of domain and package entries typed by the administrator.</p>
 */
public final class WebFilterValidator {

    private static final Pattern LABEL = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");
    private static final Pattern PACKAGE = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$");

    /**
     * <p>Public suffixes under which anyone registers domains. Blocking one of them would take down every site below
     * it, so they are refused like a bare top-level domain.</p>
     */
    private static final Set<String> PUBLIC_SUFFIXES = Set.of(
            "com.br", "net.br", "org.br", "gov.br", "edu.br", "art.br", "blog.br", "app.br", "tv.br", "leg.br",
            "jus.br", "mil.br", "sp.gov.br", "rj.gov.br", "mg.gov.br", "co.uk", "org.uk", "gov.uk", "ac.uk",
            "com.au", "net.au", "org.au", "com.ar", "com.mx", "com.pt", "co.jp", "co.in", "co.za", "com.cn",
            "github.io", "blogspot.com", "herokuapp.com", "appspot.com", "cloudfront.net", "azurewebsites.net");

    private WebFilterValidator() {
    }

    /**
     * <p>Normalizes a domain entry: lower case, without scheme, path, port, leading "*." or "." and trailing dot.
     * The resolver always applies an entry to the domain and all its subdomains.</p>
     *
     * @return the normalized domain or <code>null</code> if it is not a valid domain with at least two labels.
     */
    public static String normalizeDomain(String raw) {
        if (raw == null) {
            return null;
        }
        String d = raw.trim().toLowerCase(Locale.ROOT);
        int scheme = d.indexOf("://");
        if (scheme >= 0) {
            d = d.substring(scheme + 3);
        }
        int slash = d.indexOf('/');
        if (slash >= 0) {
            d = d.substring(0, slash);
        }
        int colon = d.indexOf(':');
        if (colon >= 0) {
            d = d.substring(0, colon);
        }
        while (d.startsWith("*.") || d.startsWith(".")) {
            d = d.substring(d.startsWith("*.") ? 2 : 1);
        }
        while (d.endsWith(".")) {
            d = d.substring(0, d.length() - 1);
        }
        if (d.isEmpty() || d.length() > 253) {
            return null;
        }
        String[] labels = d.split("\\.", -1);
        if (labels.length < 2) {
            return null;
        }
        for (String label : labels) {
            if (!LABEL.matcher(label).matches()) {
                return null;
            }
        }
        // The top-level label must not be numeric: IP literals are not filtered by DNS (design D1)
        if (labels[labels.length - 1].matches("[0-9]+")) {
            return null;
        }
        if (PUBLIC_SUFFIXES.contains(d)) {
            return null;
        }
        return d;
    }

    public static boolean isValidPackage(String pkg) {
        return pkg != null && pkg.length() <= 255 && PACKAGE.matcher(pkg).matches();
    }
}
