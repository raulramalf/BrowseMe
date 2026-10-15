package io.github.raulramalf.browseme.infrastructure.fetch;

import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import okhttp3.Dns;

/**
 * Resuelve el host una sola vez y rechaza si <em>cualquiera</em> de sus IPs está
 * prohibida. OkHttp conecta a las direcciones que devuelve este método sin
 * volver a resolver, lo que cierra la puerta al DNS rebinding entre la
 * validación y la conexión.
 */
class ValidatingDns implements Dns {

    private final HostResolver resolver;
    private final UrlPolicy policy;

    ValidatingDns(HostResolver resolver, UrlPolicy policy) {
        this.resolver = resolver;
        this.policy = policy;
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        List<InetAddress> addresses = resolver.resolve(hostname);
        if (addresses == null || addresses.isEmpty()) {
            throw new UnknownHostException(hostname);
        }
        if (addresses.stream().anyMatch(policy::isForbidden)) {
            throw new ForbiddenAddressException(hostname);
        }
        return List.copyOf(addresses);
    }
}
