package io.github.raulramalf.browseme.infrastructure.fetch;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/** Resolución DNS sustituible en tests. En producción es {@link InetAddress#getAllByName}. */
@FunctionalInterface
public interface HostResolver {

    List<InetAddress> resolve(String host) throws UnknownHostException;

    static HostResolver system() {
        return host -> List.of(InetAddress.getAllByName(host));
    }
}
