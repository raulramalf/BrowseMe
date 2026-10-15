package io.github.raulramalf.browseme.domain.page;

import java.util.Objects;

/** Encabezado H1-H6 tal como aparece en la página, con el texto ya acotado. */
public record Heading(int level, String text) {

    public static final int MAX_TEXT_LENGTH = 200;

    public Heading {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("Nivel de encabezado fuera de rango: " + level);
        }
        text = Texts.truncate(Objects.requireNonNull(text, "text"), MAX_TEXT_LENGTH);
    }
}
