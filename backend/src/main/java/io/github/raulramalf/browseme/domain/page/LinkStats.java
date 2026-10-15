package io.github.raulramalf.browseme.domain.page;

public record LinkStats(int internal, int external) {

    public LinkStats {
        if (internal < 0 || external < 0) {
            throw new IllegalArgumentException("Los recuentos de enlaces no pueden ser negativos");
        }
    }
}
