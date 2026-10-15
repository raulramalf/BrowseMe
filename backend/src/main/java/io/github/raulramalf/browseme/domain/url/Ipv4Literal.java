package io.github.raulramalf.browseme.domain.url;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Interpreta un host como IPv4 igual que lo haría un navegador o {@code inet_aton}:
 * admite de 1 a 4 partes y cada parte en decimal, octal (cero inicial) o
 * hexadecimal ({@code 0x}). Así {@code 127.1}, {@code 0x7f000001},
 * {@code 2130706433} y {@code 0177.0.0.1} se reconocen como 127.0.0.1 y no
 * esquivan la política.
 */
final class Ipv4Literal {

    private Ipv4Literal() {
    }

    /**
     * Un host «termina en número» si su última etiqueta es numérica (decimal o
     * {@code 0x...}). Según WHATWG, un host así o es una IPv4 válida o es inválido.
     */
    static boolean endsInNumber(String host) {
        String[] labels = host.split("\\.", -1);
        String last = labels[labels.length - 1];
        if (last.isEmpty()) {
            return false;
        }
        if (last.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return true;
        }
        return (last.startsWith("0x") || last.startsWith("0X"))
                && last.substring(2).chars().allMatch(c -> Character.digit(c, 16) >= 0);
    }

    /** Devuelve los 4 bytes, o vacío si no es una IPv4 válida. */
    static Optional<byte[]> parse(String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length > 4) {
            return Optional.empty();
        }
        long[] numbers = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            Optional<Long> number = parsePart(parts[i]);
            if (number.isEmpty()) {
                return Optional.empty();
            }
            numbers[i] = number.get();
        }
        for (int i = 0; i < numbers.length - 1; i++) {
            if (numbers[i] > 255) {
                return Optional.empty();
            }
        }
        long last = numbers[numbers.length - 1];
        int remainingBytes = 5 - numbers.length;
        if (last >= (1L << (8 * remainingBytes))) {
            return Optional.empty();
        }
        long value = last;
        for (int i = 0; i < numbers.length - 1; i++) {
            value += numbers[i] << (8 * (3 - i));
        }
        return Optional.of(new byte[] {
            (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value
        });
    }

    private static Optional<Long> parsePart(String part) {
        if (part.isEmpty()) {
            return Optional.empty();
        }
        int radix = 10;
        String digits = part;
        if (part.startsWith("0x") || part.startsWith("0X")) {
            radix = 16;
            digits = part.substring(2);
            if (digits.isEmpty()) {
                return Optional.of(0L);
            }
        } else if (part.length() > 1 && part.charAt(0) == '0') {
            radix = 8;
            digits = part.substring(1);
        }
        for (int i = 0; i < digits.length(); i++) {
            if (Character.digit(digits.charAt(i), radix) < 0) {
                return Optional.empty();
            }
        }
        // BigInteger evita desbordamientos con entradas como 99999999999999999999.
        BigInteger number = new BigInteger(digits, radix);
        if (number.bitLength() > 32) {
            return Optional.empty();
        }
        return Optional.of(number.longValue());
    }
}
