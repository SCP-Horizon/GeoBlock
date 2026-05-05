package fr.horizonsmp.geoBlock.geoip;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public final class MmdbDownloader {

    private static final String DOWNLOAD_URL =
            "https://download.maxmind.com/app/geoip_download"
                    + "?edition_id=%s&license_key=%s&suffix=tar.gz";

    private static final int TAR_BLOCK_SIZE = 512;
    private static final int NAME_LENGTH = 100;
    private static final int SIZE_OFFSET = 124;
    private static final int SIZE_LENGTH = 12;

    private final HttpClient http;

    public MmdbDownloader() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Downloads the given GeoLite2 edition and writes the embedded
     * .mmdb file to the given target path atomically.
     *
     * @param edition    e.g. "GeoLite2-Country" or "GeoLite2-Anonymous-IP"
     * @param licenseKey MaxMind license key, must not be blank
     * @param target     target file path; the parent directory must exist
     */
    public void download(String edition, String licenseKey, Path target) throws IOException {
        if (licenseKey == null || licenseKey.isBlank()) {
            throw new IOException("MaxMind license key is required for automatic updates");
        }

        URI uri = URI.create(String.format(DOWNLOAD_URL, edition, licenseKey));
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();

        Path tmpArchive = Files.createTempFile("geoblock-mmdb-", ".tar.gz");
        try {
            HttpResponse<Path> response = http.send(req,
                    HttpResponse.BodyHandlers.ofFile(tmpArchive));
            if (response.statusCode() != 200) {
                throw new IOException("MaxMind returned HTTP " + response.statusCode()
                        + " for " + edition + " (check the license key)");
            }
            extractMmdb(tmpArchive, target);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download was interrupted", e);
        } finally {
            try {
                Files.deleteIfExists(tmpArchive);
            } catch (IOException ignored) {
                // best-effort cleanup
            }
        }
    }

    private static void extractMmdb(Path archive, Path target) throws IOException {
        Path tmpExtract = Files.createTempFile("geoblock-mmdb-", ".mmdb");
        boolean extracted = false;
        try (InputStream in = Files.newInputStream(archive);
             GZIPInputStream gzip = new GZIPInputStream(in);
             BufferedInputStream buffered = new BufferedInputStream(gzip)) {

            byte[] header = new byte[TAR_BLOCK_SIZE];
            while (true) {
                int read = buffered.readNBytes(header, 0, TAR_BLOCK_SIZE);
                if (read < TAR_BLOCK_SIZE) {
                    break;
                }
                if (isAllZero(header)) {
                    break;
                }
                String name = readString(header, 0, NAME_LENGTH);
                long size = parseOctal(header, SIZE_OFFSET, SIZE_LENGTH);
                long padded = ((size + TAR_BLOCK_SIZE - 1) / TAR_BLOCK_SIZE) * TAR_BLOCK_SIZE;

                if (name.endsWith(".mmdb")) {
                    try (OutputStream out = Files.newOutputStream(tmpExtract)) {
                        copyExact(buffered, out, size);
                    }
                    long padding = padded - size;
                    if (padding > 0) {
                        buffered.skipNBytes(padding);
                    }
                    extracted = true;
                    break;
                } else {
                    if (padded > 0) {
                        buffered.skipNBytes(padded);
                    }
                }
            }
        }

        if (!extracted) {
            Files.deleteIfExists(tmpExtract);
            throw new IOException("Archive did not contain any .mmdb file");
        }

        Files.move(tmpExtract, target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private static void copyExact(InputStream in, OutputStream out, long bytes) throws IOException {
        byte[] buf = new byte[8192];
        long remaining = bytes;
        while (remaining > 0) {
            int toRead = (int) Math.min(buf.length, remaining);
            int r = in.read(buf, 0, toRead);
            if (r < 0) {
                throw new IOException("Unexpected end of archive while reading entry");
            }
            out.write(buf, 0, r);
            remaining -= r;
        }
    }

    private static boolean isAllZero(byte[] block) {
        for (byte b : block) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    private static String readString(byte[] block, int offset, int length) {
        int end = offset;
        int max = offset + length;
        while (end < max && block[end] != 0) {
            end++;
        }
        return new String(block, offset, end - offset, StandardCharsets.US_ASCII);
    }

    private static long parseOctal(byte[] block, int offset, int length) {
        long value = 0;
        for (int i = offset; i < offset + length; i++) {
            byte b = block[i];
            if (b == 0 || b == ' ') {
                if (value == 0) {
                    continue;
                }
                break;
            }
            if (b < '0' || b > '7') {
                break;
            }
            value = (value << 3) + (b - '0');
        }
        return value;
    }

    public static Optional<String> editionForCountry() {
        return Optional.of("GeoLite2-Country");
    }
}
