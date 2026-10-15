package io.github.raulramalf.browseme.infrastructure.fetch;

import java.net.UnknownHostException;

/**
 * El host resuelve a una dirección prohibida. Hereda de {@link UnknownHostException}
 * porque es lo que OkHttp permite lanzar desde {@code Dns.lookup}; el fetcher la
 * distingue para informar {@code URL_REJECTED} y no un fallo de DNS.
 */
class ForbiddenAddressException extends UnknownHostException {

    ForbiddenAddressException(String host) {
        super("Destino prohibido: " + host);
    }
}
