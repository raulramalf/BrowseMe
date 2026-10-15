package io.github.raulramalf.browseme.infrastructure.fetch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Lee un cuerpo de respuesta sin fiarse de {@code Content-Length}. Se cuentan
 * los bytes ya descomprimidos, así que una bomba gzip se corta igual que un
 * cuerpo enorme.
 */
final class BoundedBodyReader {

    private BoundedBodyReader() {
    }

    static final class BodyTooLargeException extends IOException {
        BodyTooLargeException(int limit) {
            super("El cuerpo supera " + limit + " bytes");
        }
    }

    /** Lee como mucho {@code limit} bytes; si hay más, lanza {@link BodyTooLargeException}. */
    static byte[] readAtMost(InputStream in, int limit) throws IOException {
        byte[] result = read(in, limit + 1);
        if (result.length > limit) {
            throw new BodyTooLargeException(limit);
        }
        return result;
    }

    /** Lee como mucho {@code limit} bytes y descarta el resto sin error (robots.txt). */
    static byte[] readTruncated(InputStream in, int limit) throws IOException {
        return read(in, limit);
    }

    private static byte[] read(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(limit, 64 * 1024));
        byte[] buffer = new byte[8192];
        int total = 0;
        while (total < limit) {
            int read = in.read(buffer, 0, Math.min(buffer.length, limit - total));
            if (read == -1) {
                break;
            }
            out.write(buffer, 0, read);
            total += read;
        }
        return out.toByteArray();
    }
}
