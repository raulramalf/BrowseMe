package io.github.raulramalf.browseme.domain.url;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class Ipv4LiteralTest {

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource({
        "127.0.0.1,      127.0.0.1",
        "127.1,          127.0.0.1",
        "127.0.1,        127.0.0.1",
        "2130706433,     127.0.0.1",
        "0x7f000001,     127.0.0.1",
        "0X7F.1,         127.0.0.1",
        "0177.0.0.01,    127.0.0.1",
        "0x,             0.0.0.0",
        "0,              0.0.0.0",
        "192.168.257,    192.168.1.1",
        "4294967295,     255.255.255.255"
    })
    void parses_alternative_ipv4_notations(String host, String expected) throws Exception {
        byte[] bytes = Ipv4Literal.parse(host).orElseThrow();

        assertThat(InetAddress.getByAddress(bytes).getHostAddress()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"256.0.0.1", "1.2.3.4.5", "4294967296", "1..1", "08.0.0.1", "0x1g", "1.2.3.256", "a.1"})
    void rejects_invalid_ipv4(String host) {
        assertThat(Ipv4Literal.parse(host)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "example.com,  false",
        "1.2.3.4,      true",
        "foo.0x1f,     true",
        "foo.0xzz,     false",
        "foo.123,      true",
        "123.foo,      false"
    })
    void detects_hosts_ending_in_a_number(String host, boolean expected) {
        assertThat(Ipv4Literal.endsInNumber(host)).isEqualTo(expected);
    }
}
