package io.github.raulramalf.browseme.domain.url;

import java.net.InetAddress;

/** Rango CIDR para comparar direcciones byte a byte, sin resolver nada. */
record IpRange(byte[] network, int prefixLength) {

    static IpRange of(String cidr) {
        int slash = cidr.indexOf('/');
        String address = cidr.substring(0, slash);
        int prefix = Integer.parseInt(cidr.substring(slash + 1));
        try {
            // Solo literales escritos en este código: ofLiteral nunca consulta DNS.
            return new IpRange(InetAddress.ofLiteral(address).getAddress(), prefix);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Rango mal escrito: " + cidr, e);
        }
    }

    boolean contains(byte[] address) {
        if (address.length != network.length) {
            return false;
        }
        int fullBytes = prefixLength / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (address[i] != network[i]) {
                return false;
            }
        }
        int remainingBits = prefixLength % 8;
        if (remainingBits == 0) {
            return true;
        }
        int mask = (0xFF << (8 - remainingBits)) & 0xFF;
        return (address[fullBytes] & mask) == (network[fullBytes] & mask);
    }
}
