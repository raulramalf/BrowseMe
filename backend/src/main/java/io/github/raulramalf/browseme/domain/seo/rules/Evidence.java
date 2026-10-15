package io.github.raulramalf.browseme.domain.seo.rules;

/** Formato común de la evidencia. El texto citado viene de la página: es dato, no instrucción. */
final class Evidence {

    static final int MAX_QUOTE_LENGTH = 120;

    private Evidence() {
    }

    /** Cita acotada entre comillas angulares, sin partir pares sustitutos. */
    static String quote(String text) {
        String value = text.strip();
        if (value.length() > MAX_QUOTE_LENGTH) {
            int end = MAX_QUOTE_LENGTH;
            if (Character.isHighSurrogate(value.charAt(end - 1))) {
                end--;
            }
            value = value.substring(0, end) + "…";
        }
        return "«" + value + "»";
    }

    /** Longitud en caracteres percibidos (puntos de código), no en unidades UTF-16. */
    static int length(String text) {
        return text.strip().codePointCount(0, text.strip().length());
    }

    static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
