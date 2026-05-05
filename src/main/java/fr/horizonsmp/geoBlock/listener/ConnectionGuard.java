package fr.horizonsmp.geoBlock.listener;

import fr.horizonsmp.geoBlock.bypass.BypassStore;
import fr.horizonsmp.geoBlock.config.FailurePolicy;
import fr.horizonsmp.geoBlock.config.FilterMode;
import fr.horizonsmp.geoBlock.config.PluginConfig;
import fr.horizonsmp.geoBlock.geoip.GeoIpService;
import fr.horizonsmp.geoBlock.geoip.LookupResult;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;

public final class ConnectionGuard {

    private final BypassStore bypassStore;
    private final GeoIpService geoIpService;
    private final java.util.function.Supplier<PluginConfig> configSupplier;

    public ConnectionGuard(BypassStore bypassStore,
                           GeoIpService geoIpService,
                           java.util.function.Supplier<PluginConfig> configSupplier) {
        this.bypassStore = bypassStore;
        this.geoIpService = geoIpService;
        this.configSupplier = configSupplier;
    }

    public ConnectionDecision evaluate(UUID uuid, InetAddress address) {
        if (uuid != null && bypassStore.containsUuid(uuid)) {
            return ConnectionDecision.allow();
        }
        if (address != null && bypassStore.matchesIp(address)) {
            return ConnectionDecision.allow();
        }

        PluginConfig config = configSupplier.get();
        Optional<LookupResult> lookup = geoIpService.lookup(address);

        if (lookup.isEmpty()) {
            return config.onLookupFailure() == FailurePolicy.DENY
                    ? ConnectionDecision.deny(DenialReason.LOOKUP_FAILED)
                    : ConnectionDecision.allow();
        }

        LookupResult result = lookup.get();
        String iso = result.countryIso();
        boolean proxy = result.proxy();

        if (proxy && config.vpnDetection().enabled() && config.vpnDetection().blockOnDetection()) {
            return ConnectionDecision.deny(DenialReason.VPN_DETECTED, iso, true);
        }

        boolean inList = config.countries().contains(iso);
        if (config.mode() == FilterMode.BLACKLIST) {
            if (inList) {
                return ConnectionDecision.deny(DenialReason.COUNTRY_BLACKLISTED, iso, proxy);
            }
        } else if (config.mode() == FilterMode.WHITELIST) {
            if (!inList) {
                return ConnectionDecision.deny(DenialReason.COUNTRY_NOT_WHITELISTED, iso, proxy);
            }
        }

        return ConnectionDecision.allow(iso, proxy);
    }
}
