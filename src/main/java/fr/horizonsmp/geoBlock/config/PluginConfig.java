package fr.horizonsmp.geoBlock.config;

import java.util.Set;

public record PluginConfig(
        FilterMode mode,
        Set<String> countries,
        FailurePolicy onLookupFailure,
        GeoIp geoip,
        VpnDetection vpnDetection,
        Discord discord
) {

    public record GeoIp(
            String licenseKey,
            boolean autoUpdate,
            long updateIntervalHours,
            String databasePath
    ) {
    }

    public record VpnDetection(
            boolean enabled,
            String databasePath,
            boolean blockOnDetection
    ) {
    }

    public record Discord(
            String webhookUrl,
            boolean includeIp,
            String username
    ) {
        public boolean isEnabled() {
            return webhookUrl != null && !webhookUrl.isBlank();
        }
    }
}
