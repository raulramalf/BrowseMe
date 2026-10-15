package io.github.raulramalf.browseme.domain.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Intérprete simplificado de robots.txt según RFC 9309: grupos por
 * {@code user-agent}, reglas {@code allow}/{@code disallow} con comodines
 * {@code *} y {@code $}, gana la regla más específica y, a igualdad, {@code allow}.
 *
 * <p>Limitación declarada: no normaliza escapes porcentuales ni aplica extensiones
 * propias de cada buscador.
 */
public final class RobotsTxt {

    /** RFC 9309: los rastreadores deben procesar al menos 500 KiB. */
    public static final int MAX_PARSED_BYTES = 500 * 1024;

    private final List<Group> groups;

    private RobotsTxt(List<Group> groups) {
        this.groups = groups;
    }

    public static RobotsTxt parse(String content) {
        Objects.requireNonNull(content, "content");
        List<Group> groups = new ArrayList<>();
        Group current = null;
        boolean lastLineWasUserAgent = false;

        for (String rawLine : content.split("\\r?\\n|\\r")) {
            int comment = rawLine.indexOf('#');
            String line = (comment >= 0 ? rawLine.substring(0, comment) : rawLine).strip();
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = line.substring(0, colon).strip().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).strip();

            switch (key) {
                case "user-agent" -> {
                    if (current == null || !lastLineWasUserAgent) {
                        current = new Group();
                        groups.add(current);
                    }
                    current.agents.add(value.toLowerCase(Locale.ROOT));
                    lastLineWasUserAgent = true;
                }
                case "allow", "disallow" -> {
                    if (current != null && !value.isEmpty()) {
                        current.rules.add(new Rule(value, key.equals("allow")));
                    }
                    lastLineWasUserAgent = false;
                }
                default -> lastLineWasUserAgent = false;
            }
        }
        return new RobotsTxt(List.copyOf(groups));
    }

    /** Si algún grupo nombra expresamente a este agente (sin contar {@code *}). */
    public boolean namesAgent(String agent) {
        String token = agent.toLowerCase(Locale.ROOT);
        return groups.stream().anyMatch(g -> g.agents.contains(token));
    }

    /**
     * @param agent        token de producto del rastreador, por ejemplo {@code googlebot}
     * @param pathAndQuery ruta con consulta, empezando por {@code /}
     */
    public boolean isAllowed(String agent, String pathAndQuery) {
        if (pathAndQuery.equals("/robots.txt")) {
            return true;
        }
        String token = agent.toLowerCase(Locale.ROOT);
        List<Rule> rules = rulesFor(token);
        if (rules.isEmpty()) {
            rules = rulesFor("*");
        }
        Rule best = null;
        for (Rule rule : rules) {
            if (rule.matches(pathAndQuery) && (best == null
                    || rule.pattern.length() > best.pattern.length()
                    || (rule.pattern.length() == best.pattern.length() && rule.allow))) {
                best = rule;
            }
        }
        return best == null || best.allow;
    }

    private List<Rule> rulesFor(String token) {
        List<Rule> rules = new ArrayList<>();
        boolean matched = false;
        for (Group group : groups) {
            if (group.agents.contains(token)) {
                matched = true;
                rules.addAll(group.rules);
            }
        }
        // Un grupo que coincide pero no tiene reglas permite todo: se distingue de «no hay grupo».
        return matched && rules.isEmpty() ? List.of(new Rule("/", true)) : rules;
    }

    private static final class Group {
        private final List<String> agents = new ArrayList<>();
        private final List<Rule> rules = new ArrayList<>();
    }

    private record Rule(String pattern, boolean allow) {

        boolean matches(String path) {
            // Sin '$' la regla es un prefijo: equivale a terminar en '*'.
            String glob = pattern.endsWith("$") ? pattern.substring(0, pattern.length() - 1) : pattern + "*";
            return globMatches(glob, path);
        }

        /**
         * Comodín '*' con retroceso al último '*' visto: O(n·m) en el peor caso.
         * El robots.txt viene de la web analizada, así que una versión recursiva
         * exponencial sería una vía de denegación de servicio.
         */
        private static boolean globMatches(String glob, String text) {
            int g = 0;
            int t = 0;
            int starGlob = -1;
            int starText = -1;
            while (t < text.length()) {
                if (g < glob.length() && glob.charAt(g) == '*') {
                    starGlob = g++;
                    starText = t;
                } else if (g < glob.length() && glob.charAt(g) == text.charAt(t)) {
                    g++;
                    t++;
                } else if (starGlob >= 0) {
                    g = starGlob + 1;
                    t = ++starText;
                } else {
                    return false;
                }
            }
            while (g < glob.length() && glob.charAt(g) == '*') {
                g++;
            }
            return g == glob.length();
        }
    }
}
