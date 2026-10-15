package io.github.raulramalf.browseme.domain.url;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Política de URLs admitidas para análisis. Lógica pura: nunca resuelve DNS.
 *
 * <p>{@link #parse(String)} es la primera barrera (respuesta rápida al usuario)
 * y rechaza los destinos que se conocen sin resolver: IPs literales en
 * cualquier formato y nombres locales. La barrera real es
 * {@link #isForbidden(InetAddress)}, que el fetcher aplica a <em>todas</em> las
 * IPs resueltas en cada salto antes de conectar.
 *
 * <p>Una expresión regular no basta: el host se normaliza (IDN, formatos IPv4
 * alternativos, IPv6) antes de decidir.
 */
public final class UrlPolicy {

    public static final int MAX_URL_LENGTH = 2048;

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<Integer> ALLOWED_PORTS = Set.of(80, 443);

    private static final Pattern DNS_LABEL = Pattern.compile("[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?");
    private static final Pattern IPV6_LITERAL_CHARS = Pattern.compile("[0-9a-f:.]+");
    private static final Pattern PORT = Pattern.compile("[0-9]{1,5}");

    /** Sufijos que solo tienen sentido en redes locales (RFC 6761, RFC 6762, RFC 8375 y usos de facto). */
    private static final List<String> LOCAL_SUFFIXES =
            List.of("localhost", "local", "internal", "home.arpa", "lan", "localdomain");

    /** Rangos IPv4 de uso especial (registro IANA) que no son destinos públicos. */
    private static final List<IpRange> FORBIDDEN_IPV4 = List.of(
            IpRange.of("0.0.0.0/8"),          // "esta red" y no especificada
            IpRange.of("10.0.0.0/8"),         // privada RFC 1918
            IpRange.of("100.64.0.0/10"),      // CGNAT
            IpRange.of("127.0.0.0/8"),        // loopback
            IpRange.of("169.254.0.0/16"),     // link-local, incluye 169.254.169.254 (metadatos cloud)
            IpRange.of("172.16.0.0/12"),      // privada RFC 1918
            IpRange.of("192.0.0.0/24"),       // asignaciones de protocolo IETF
            IpRange.of("192.0.2.0/24"),       // documentación
            IpRange.of("192.88.99.0/24"),     // relay 6to4 (obsoleto)
            IpRange.of("192.168.0.0/16"),     // privada RFC 1918
            IpRange.of("198.18.0.0/15"),      // pruebas de rendimiento
            IpRange.of("198.51.100.0/24"),    // documentación
            IpRange.of("203.0.113.0/24"),     // documentación
            IpRange.of("224.0.0.0/4"),        // multicast
            IpRange.of("240.0.0.0/4"));       // reservada y broadcast

    /**
     * IPv6 se trata por lista blanca: solo unicast global (2000::/3). Así quedan
     * fuera loopback, no especificada, IPv4 mapeada o compatible, NAT64
     * (64:ff9b::/96), ULA (fc00::/7), link-local, site-local y multicast.
     */
    private static final IpRange IPV6_GLOBAL_UNICAST = IpRange.of("2000::/3");

    private static final List<IpRange> FORBIDDEN_IPV6_IN_GLOBAL = List.of(
            IpRange.of("2001::/23"),          // asignaciones de protocolo IETF (Teredo, ORCHID...)
            IpRange.of("2001:db8::/32"),      // documentación
            IpRange.of("2002::/16"),          // 6to4: lleva una IPv4 incrustada
            IpRange.of("3fff::/20"));         // documentación (RFC 9637)

    /**
     * Valida y normaliza una URL introducida por el usuario.
     *
     * @throws InvalidUrlException  si no es una URL admisible
     * @throws UrlRejectedException si apunta a un destino prohibido conocido sin resolver DNS
     */
    public AnalyzedUrl parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidUrlException("La URL está vacía");
        }
        String input = raw.strip();
        if (input.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("La URL supera " + MAX_URL_LENGTH + " caracteres");
        }
        if (input.chars().anyMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c) || c == '\\')) {
            throw new InvalidUrlException("La URL contiene caracteres no permitidos");
        }

        // La autoridad se separa a mano: java.net.URI no acepta hosts no ASCII
        // (münchen.de) y aquí deben llegar a IDN antes de validarse.
        int schemeEnd = input.indexOf("://");
        String scheme = schemeEnd <= 0 ? null : input.substring(0, schemeEnd).toLowerCase(Locale.ROOT);
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme)) {
            throw new InvalidUrlException("Solo se admiten URLs http y https");
        }
        String afterScheme = input.substring(schemeEnd + 3);
        int authorityEnd = afterScheme.length();
        for (char delimiter : new char[] {'/', '?', '#'}) {
            int index = afterScheme.indexOf(delimiter);
            if (index >= 0 && index < authorityEnd) {
                authorityEnd = index;
            }
        }
        String authority = afterScheme.substring(0, authorityEnd);
        if (authority.isEmpty()) {
            throw new InvalidUrlException("La URL no indica un servidor");
        }
        URI uri;
        try {
            // Solo para separar ruta y consulta; el host de este URI no se usa.
            uri = new URI("http://host.invalid" + afterScheme.substring(authorityEnd));
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("La URL no tiene un formato válido");
        }

        if (authority.contains("@")) {
            throw new InvalidUrlException("La URL no puede llevar usuario ni contraseña");
        }
        HostAndPort hostAndPort = splitAuthority(authority);
        String normalizedHost = normalizeHost(hostAndPort.host());

        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        String port = hostAndPort.port().filter(p -> p != defaultPort(scheme)).map(p -> ":" + p).orElse("");

        String normalized = scheme + "://" + normalizedHost + port + path + query;
        URI result;
        try {
            result = new URI(new URI(normalized).toASCIIString());
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("La URL no tiene un formato válido");
        }
        if (result.toString().length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("La URL supera " + MAX_URL_LENGTH + " caracteres");
        }
        return new AnalyzedUrl(result);
    }

    /**
     * Si una IP es un destino prohibido. El fetcher debe rechazar el host si
     * <em>cualquiera</em> de sus IPs resueltas lo es.
     */
    public boolean isForbidden(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            return FORBIDDEN_IPV4.stream().anyMatch(range -> range.contains(bytes));
        }
        if (address instanceof Inet6Address) {
            if (!IPV6_GLOBAL_UNICAST.contains(bytes)) {
                return true;
            }
            return FORBIDDEN_IPV6_IN_GLOBAL.stream().anyMatch(range -> range.contains(bytes));
        }
        return true;
    }

    private record HostAndPort(String host, Optional<Integer> port) {
    }

    private static HostAndPort splitAuthority(String authority) {
        String host;
        String portPart;
        if (authority.startsWith("[")) {
            int end = authority.indexOf(']');
            if (end < 0) {
                throw new InvalidUrlException("Dirección IPv6 mal cerrada");
            }
            host = authority.substring(0, end + 1);
            String rest = authority.substring(end + 1);
            if (!rest.isEmpty() && !rest.startsWith(":")) {
                throw new InvalidUrlException("La URL no tiene un formato válido");
            }
            portPart = rest.isEmpty() ? null : rest.substring(1);
        } else {
            int colon = authority.lastIndexOf(':');
            host = colon < 0 ? authority : authority.substring(0, colon);
            portPart = colon < 0 ? null : authority.substring(colon + 1);
        }
        if (portPart == null) {
            return new HostAndPort(host, Optional.empty());
        }
        if (!PORT.matcher(portPart).matches()) {
            throw new InvalidUrlException("Puerto no válido");
        }
        int port = Integer.parseInt(portPart);
        if (!ALLOWED_PORTS.contains(port)) {
            throw new InvalidUrlException("Solo se admiten los puertos 80 y 443");
        }
        return new HostAndPort(host, Optional.of(port));
    }

    /** Devuelve el host listo para la URL normalizada, o lanza si no se admite. */
    private String normalizeHost(String rawHost) {
        if (rawHost.isEmpty()) {
            throw new InvalidUrlException("La URL no indica un servidor");
        }
        if (rawHost.contains("%")) {
            // Hosts con escapes (%31%32%37...) o identificadores de zona IPv6.
            throw new InvalidUrlException("El nombre del servidor no puede llevar caracteres codificados");
        }
        if (rawHost.startsWith("[")) {
            return normalizeIpv6(rawHost.substring(1, rawHost.length() - 1).toLowerCase(Locale.ROOT));
        }

        String host = rawHost.endsWith(".") ? rawHost.substring(0, rawHost.length() - 1) : rawHost;
        String ascii;
        try {
            // IDN también convierte dígitos de ancho completo y puntos ideográficos,
            // así que la detección de IPv4 se hace después.
            ascii = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            throw new InvalidUrlException("El nombre del servidor no es válido");
        }
        if (ascii.endsWith(".")) {
            ascii = ascii.substring(0, ascii.length() - 1);
        }
        if (ascii.isEmpty()) {
            throw new InvalidUrlException("La URL no indica un servidor");
        }

        if (Ipv4Literal.endsInNumber(ascii)) {
            byte[] bytes = Ipv4Literal.parse(ascii)
                    .orElseThrow(() -> new InvalidUrlException("Dirección IPv4 no válida"));
            InetAddress address = toAddress(bytes);
            if (isForbidden(address)) {
                throw new UrlRejectedException("La dirección apunta a una red privada o reservada");
            }
            return address.getHostAddress();
        }

        return normalizeDomainName(ascii);
    }

    private String normalizeIpv6(String literal) {
        if (!IPV6_LITERAL_CHARS.matcher(literal).matches()) {
            throw new InvalidUrlException("Dirección IPv6 no válida");
        }
        InetAddress address;
        try {
            address = Inet6Address.ofLiteral(literal);
        } catch (IllegalArgumentException e) {
            throw new InvalidUrlException("Dirección IPv6 no válida");
        }
        if (!(address instanceof Inet6Address) || isForbidden(address)) {
            // Una IPv4 mapeada (::ffff:a.b.c.d) se trata como IPv6 no global.
            throw new UrlRejectedException("La dirección apunta a una red privada o reservada");
        }
        return "[" + address.getHostAddress() + "]";
    }

    private static String normalizeDomainName(String ascii) {
        if (ascii.length() > 253) {
            throw new InvalidUrlException("El nombre del servidor es demasiado largo");
        }
        String[] labels = ascii.split("\\.", -1);
        for (String label : labels) {
            if (!DNS_LABEL.matcher(label).matches()) {
                throw new InvalidUrlException("El nombre del servidor no es válido");
            }
        }
        if (labels.length < 2) {
            throw new UrlRejectedException("Solo se admiten dominios públicos");
        }
        for (String suffix : LOCAL_SUFFIXES) {
            if (ascii.equals(suffix) || ascii.endsWith("." + suffix)) {
                throw new UrlRejectedException("Solo se admiten dominios públicos");
            }
        }
        return ascii;
    }

    private static InetAddress toAddress(byte[] ipv4) {
        return Inet4Address.ofLiteral((ipv4[0] & 0xFF) + "." + (ipv4[1] & 0xFF) + "."
                + (ipv4[2] & 0xFF) + "." + (ipv4[3] & 0xFF));
    }

    private static int defaultPort(String scheme) {
        return "https".equals(scheme) ? 443 : 80;
    }
}
