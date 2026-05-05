package fr.horizonsmp.geoBlock.config;

import java.util.Locale;

public enum FilterMode {
    BLACKLIST,
    WHITELIST;

    public static FilterMode fromString(String raw, FilterMode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return FilterMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
