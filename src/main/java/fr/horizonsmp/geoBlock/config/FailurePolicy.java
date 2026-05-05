package fr.horizonsmp.geoBlock.config;

import java.util.Locale;

public enum FailurePolicy {
    ALLOW,
    DENY;

    public static FailurePolicy fromString(String raw, FailurePolicy fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return FailurePolicy.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
