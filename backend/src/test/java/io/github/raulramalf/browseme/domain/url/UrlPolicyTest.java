package io.github.raulramalf.browseme.domain.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UrlPolicyTest {

    private final UrlPolicy policy = new UrlPolicy();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "https://example.com,                       https://example.com/",
        "HTTPS://Example.COM:443/Ruta?q=1#ancla,     https://example.com/Ruta?q=1",
        "http://example.com:80,                      http://example.com/",
        "http://example.com:443/,                    http://example.com:443/",
        "https://example.com.,                       https://example.com/",
        "https://sub.dominio-ejemplo.es/a/b,         https://sub.dominio-ejemplo.es/a/b",
        "https://münchen.de,                         https://xn--mnchen-3ya.de/",
        "https://example.com/café,                   https://example.com/caf%C3%A9",
        "http://8.8.8.8,                             http://8.8.8.8/",
        "https://[2001:4860:4860::8888]/,            https://[2001:4860:4860:0:0:0:0:8888]/",
        "'  https://example.com  ',                  https://example.com/"
    })
    void accepts_and_normalizes_public_urls(String raw, String expected) {
        AnalyzedUrl url = policy.parse(raw);

        assertThat(url.value()).isEqualTo(expected);
    }

    @Test
    void exposes_host_and_effective_port() {
        AnalyzedUrl url = policy.parse("https://[2001:4860:4860::8888]/");

        assertThat(url.host()).isEqualTo("2001:4860:4860:0:0:0:0:8888");
        assertThat(url.port()).isEqualTo(443);
        assertThat(policy.parse("http://example.com").port()).isEqualTo(80);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "   ",
        "example.com",                          // sin esquema
        "ftp://example.com",
        "file:///etc/passwd",
        "javascript:alert(1)",
        "gopher://example.com",
        "http:example.com",
        "https://",
        "https:///ruta",
        "http://user:pass@example.com",         // credenciales embebidas
        "http://@example.com",
        "https://example.com:8080",             // puerto no permitido
        "https://example.com:22",
        "https://example.com:0443x",
        "https://example.com:",
        "https://exa mple.com",
        "https://example.com\\@evil.com",
        "https://example.com/\nruta",
        "http://%31%32%37.0.0.1/",              // host con escapes
        "http://[fe80::1%25eth0]/",             // identificador de zona
        "http://256.256.256.256",               // termina en número pero no es IPv4
        "http://1.2.3.4.5",
        "http://0x100000000",
        "http://99999999999999999999",
        "http://[::1",
        "http://[zzzz::1]",
        "http://ex_ample.com",
        "http://-example.com",
        "http://example..com"
    })
    void rejects_malformed_or_unsupported_urls(String raw) {
        assertThatThrownBy(() -> policy.parse(raw)).isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejects_urls_longer_than_the_limit() {
        String raw = "https://example.com/" + "a".repeat(UrlPolicy.MAX_URL_LENGTH);

        assertThatThrownBy(() -> policy.parse(raw)).isInstanceOf(InvalidUrlException.class);
    }

    /** Batería SSRF del apartado 11: destinos prohibidos detectables sin DNS. */
    @ParameterizedTest
    @ValueSource(strings = {
        "http://127.0.0.1",
        "http://127.1",                         // forma abreviada
        "http://0x7f000001",                    // hexadecimal
        "http://0x7f.0x0.0x0.0x1",
        "http://2130706433",                    // decimal entero
        "http://0177.0.0.1",                    // octal
        "http://017700000001",
        "http://127.0.0.1.",
        "http://１２７.０.０.１",                // dígitos de ancho completo
        "http://[::1]",
        "http://[::]",
        "http://[::ffff:127.0.0.1]",            // IPv4 mapeada en IPv6
        "http://[::ffff:7f00:1]",
        "http://[::127.0.0.1]",                 // IPv4 compatible (obsoleta)
        "http://[64:ff9b::7f00:1]",             // NAT64
        "http://[2002:7f00:1::]",               // 6to4 con loopback incrustada
        "http://[2001:0:4136:e378:8000:63bf:3fff:fdd2]", // Teredo
        "http://[fd00::1]",                     // ULA
        "http://[fe80::1]",                     // link-local
        "http://[ff02::1]",                     // multicast
        "http://169.254.169.254/latest/meta-data/",
        "http://0.0.0.0",
        "http://0",
        "http://10.0.0.1",
        "http://172.16.0.1",
        "http://172.31.255.255",
        "http://192.168.1.1",
        "http://100.64.0.1",
        "http://224.0.0.1",
        "http://255.255.255.255",
        "http://localhost",
        "http://LOCALHOST.",
        "http://app.localhost",
        "http://impresora.local",
        "http://servicio.internal",
        "http://router.home.arpa",
        "http://intranet"                       // nombre de una sola etiqueta
    })
    void rejects_forbidden_destinations(String raw) {
        assertThatThrownBy(() -> policy.parse(raw)).isInstanceOf(UrlRejectedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.0.1", "169.254.169.254", "100.64.0.1",
        "0.0.0.0", "224.0.0.251", "255.255.255.255", "192.0.2.10", "198.18.0.1",
        "::1", "::", "fc00::1", "fd12:3456::1", "fe80::1", "ff02::1",
        "64:ff9b::a00:1", "2001:db8::1", "2002:a00:1::1", "2001::1"
    })
    void forbids_private_and_reserved_addresses(String literal) {
        assertThat(policy.isForbidden(InetAddress.ofLiteral(literal))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "8.8.8.8", "1.1.1.1", "93.184.215.14", "172.32.0.1", "100.128.0.1", "11.0.0.1",
        "2001:4860:4860::8888", "2a00:1450:4003:80f::200e", "2606:4700::6810:84e5"
    })
    void allows_public_addresses(String literal) {
        assertThat(policy.isForbidden(InetAddress.ofLiteral(literal))).isFalse();
    }
}
