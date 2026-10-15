package io.github.raulramalf.browseme.infrastructure.fetch;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Verbos y expresiones habituales de llamada a la acción, en español e inglés.
 * El mismo patrón se usa en Java (HTML) y en el navegador (página dibujada), por
 * eso solo usa sintaxis común a ambos motores de expresiones regulares y se
 * aplica sobre texto normalizado: minúsculas y sin tildes.
 */
public final class CallToActionVocabulary {

    /** Patrón sin delimitadores ni banderas, válido en Java y en JavaScript. */
    public static final String PATTERN = "\\b(?:reserv\\w*|compra\\w*|compre\\w*|pide\\w*|pidelo|pedir|"
            + "haz tu pedido|encarg\\w*|contact\\w*|llama\\w*|llamenos|solicit\\w*|presupuesto\\w*|"
            + "suscrib\\w*|apuntate|registr\\w*|descarg\\w*|empie[cz]\\w*|empez\\w*|prueba\\w* gratis|"
            + "pruebalo|agend\\w*|cita previa|pide cita|inscrib\\w*|hazte\\w*|unete|escribe\\w*|whatsapp|"
            + "book\\w*|buy\\w*|order\\w*|call (?:us|now)|get started|sign ?up|subscribe\\w*|request\\w*|"
            + "get a quote|shop\\w*|try (?:it )?(?:for )?free|join\\w*|download\\w*|schedule\\w*)\\b";

    /** Textos más largos no son botones ni enlaces de acción, sino párrafos enlazados. */
    public static final int MAX_TEXT_LENGTH = 60;

    private static final Pattern COMPILED = Pattern.compile(PATTERN);
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private CallToActionVocabulary() {
    }

    public static boolean matches(String text) {
        if (text == null) {
            return false;
        }
        String normalized = normalize(text);
        return !normalized.isEmpty() && normalized.length() <= MAX_TEXT_LENGTH
                && COMPILED.matcher(normalized).find();
    }

    /** Minúsculas, sin tildes y con espacios simples. */
    public static String normalize(String text) {
        String withoutMarks = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        return withoutMarks.toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
    }
}
