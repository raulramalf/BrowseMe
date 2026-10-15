package io.github.raulramalf.browseme.domain.page;

import java.util.Arrays;
import java.util.Objects;

/**
 * Análisis visual: estado, métricas (solo si {@code DONE}) y capturas JPEG de la
 * primera pantalla en móvil y en escritorio (pueden faltar).
 */
public record VisualCheck(VisualStatus status, VisualMetrics metrics, byte[] mobileScreenshot,
                          byte[] desktopScreenshot) {

    /** Tope por captura; una mayor se descarta en lugar de guardarse. */
    public static final int MAX_SCREENSHOT_BYTES = 600 * 1024;

    public VisualCheck {
        Objects.requireNonNull(status, "status");
        if (status == VisualStatus.DONE && metrics == null) {
            throw new IllegalArgumentException("Un análisis visual hecho necesita métricas");
        }
        if (status != VisualStatus.DONE) {
            metrics = null;
            mobileScreenshot = null;
            desktopScreenshot = null;
        }
        mobileScreenshot = bounded(mobileScreenshot);
        desktopScreenshot = bounded(desktopScreenshot);
    }

    public static VisualCheck notRun() {
        return of(VisualStatus.NOT_RUN);
    }

    public static VisualCheck of(VisualStatus status) {
        return new VisualCheck(status, null, null, null);
    }

    public static VisualCheck done(VisualMetrics metrics, byte[] mobile, byte[] desktop) {
        return new VisualCheck(VisualStatus.DONE, metrics, mobile, desktop);
    }

    public boolean done() {
        return status == VisualStatus.DONE;
    }

    @Override
    public byte[] mobileScreenshot() {
        return mobileScreenshot == null ? null : mobileScreenshot.clone();
    }

    @Override
    public byte[] desktopScreenshot() {
        return desktopScreenshot == null ? null : desktopScreenshot.clone();
    }

    private static byte[] bounded(byte[] image) {
        if (image == null || image.length == 0 || image.length > MAX_SCREENSHOT_BYTES) {
            return null;
        }
        return image.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VisualCheck that && status == that.status && Objects.equals(metrics, that.metrics)
                && Arrays.equals(mobileScreenshot, that.mobileScreenshot)
                && Arrays.equals(desktopScreenshot, that.desktopScreenshot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, metrics, Arrays.hashCode(mobileScreenshot), Arrays.hashCode(desktopScreenshot));
    }

    @Override
    public String toString() {
        return "VisualCheck[status=" + status + ", metrics=" + metrics + "]";
    }
}
