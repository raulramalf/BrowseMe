package io.github.raulramalf.browseme.domain.url;

import java.net.URI;
import java.util.Objects;

/**
 * URL ya normalizada y aceptada por {@link UrlPolicy}. Solo se puede obtener
 * desde la política, así que su existencia garantiza que pasó la validación
 * sintáctica. La validación de las IPs resueltas se repite al descargar.
 */
public final class AnalyzedUrl {

    private final URI uri;

    AnalyzedUrl(URI uri) {
        this.uri = Objects.requireNonNull(uri, "uri");
    }

    public URI uri() {
        return uri;
    }

    /** Host en ASCII (punycode); las IPv6 van sin corchetes. */
    public String host() {
        String host = uri.getHost();
        return host.startsWith("[") ? host.substring(1, host.length() - 1) : host;
    }

    public int port() {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equals(uri.getScheme()) ? 443 : 80;
    }

    public String value() {
        return uri.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AnalyzedUrl that && uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public String toString() {
        return value();
    }
}
