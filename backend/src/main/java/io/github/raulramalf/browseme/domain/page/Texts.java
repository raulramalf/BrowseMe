package io.github.raulramalf.browseme.domain.page;

/** Utilidades de texto para acotar lo que se guarda de una página no confiable. */
final class Texts {

    private Texts() {
    }

    /** Recorta sin partir un par sustituto UTF-16 y normaliza espacios. */
    static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        int end = maxLength;
        if (Character.isHighSurrogate(normalized.charAt(end - 1))) {
            end--;
        }
        return normalized.substring(0, end);
    }
}
