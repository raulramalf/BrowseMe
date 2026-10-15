package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

/**
 * La canónica apunta a otra URL. Se comparan esquema y host sin mayúsculas, sin
 * puerto por defecto ni fragmento; la ruta y la consulta se comparan tal cual.
 */
public final class CanonicalDifferentRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.CANONICAL_DIFFERENT;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (Evidence.isBlank(snapshot.canonical())) {
            return Optional.empty();
        }
        Optional<String> canonical = normalize(snapshot.canonical().strip());
        if (canonical.isEmpty()) {
            return issue("La URL canónica declarada no es una dirección válida: "
                            + Evidence.quote(snapshot.canonical()) + ".",
                    "Los buscadores ignoran una canónica que no pueden interpretar.",
                    "Corrige el atributo href de <link rel=\"canonical\"> con una URL absoluta.");
        }
        if (canonical.equals(normalize(snapshot.finalUrl().toString()))) {
            return Optional.empty();
        }
        return issue("La URL canónica " + Evidence.quote(snapshot.canonical())
                        + " es distinta de la analizada " + Evidence.quote(snapshot.finalUrl().toString()) + ".",
                "Puede ser intencionado, por ejemplo para unificar varias versiones de la misma página.",
                "Comprueba que la canónica apunta a la versión que quieres que aparezca en los buscadores.");
    }

    private static Optional<String> normalize(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return Optional.empty();
            }
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            boolean defaultPort = port == -1 || (scheme.equals("http") && port == 80)
                    || (scheme.equals("https") && port == 443);
            String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
            return Optional.of(scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                    + (defaultPort ? "" : ":" + port) + path + query);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }
}
