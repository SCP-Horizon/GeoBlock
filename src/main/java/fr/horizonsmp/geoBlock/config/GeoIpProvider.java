package fr.horizonsmp.geoBlock.config;

import java.util.Locale;

public enum GeoIpProvider {
    /** Free, no-account MMDB from db-ip.com (CC-BY 4.0). Default. */
    DB_IP,
    /** MaxMind GeoLite2-Country MMDB. Requires a free MaxMind license key. */
    MAXMIND;

    public static GeoIpProvider fromString(String raw, GeoIpProvider fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "db-ip", "dbip" -> DB_IP;
            case "maxmind", "geolite2", "geolite" -> MAXMIND;
            default -> fallback;
        };
    }
}
