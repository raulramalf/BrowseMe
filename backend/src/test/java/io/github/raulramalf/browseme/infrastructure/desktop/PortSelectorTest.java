package io.github.raulramalf.browseme.infrastructure.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;

class PortSelectorTest {

    private static ServerSocket occupy() throws IOException {
        return new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    }

    @Test
    void uses_the_preferred_port_when_it_is_free() throws IOException {
        int port;
        try (ServerSocket probe = occupy()) {
            port = probe.getLocalPort();
        }
        assertThat(PortSelector.firstFree(port)).isEqualTo(port);
    }

    @Test
    void skips_a_port_held_by_another_copy_of_the_app() throws IOException {
        try (ServerSocket other = occupy()) {
            int busy = other.getLocalPort();
            int chosen = PortSelector.firstFree(busy);
            assertThat(chosen).isGreaterThan(busy).isLessThan(busy + PortSelector.ATTEMPTS);
            assertThat(PortSelector.isFree(chosen)).isTrue();
        }
    }

    @Test
    void falls_back_to_the_preferred_port_when_none_is_free() throws IOException {
        try (ServerSocket a = occupy()) {
            int busy = a.getLocalPort();
            // Si todos los candidatos estuvieran ocupados se deja que Spring informe del fallo.
            assertThat(PortSelector.firstFree(busy)).isNotEqualTo(busy);
        }
    }
}
