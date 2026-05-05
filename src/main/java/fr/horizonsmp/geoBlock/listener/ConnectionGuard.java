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
import java.util.function.Supplier;

public final class ConnectionGuard {

    private final BypassStore bypassStore;
    private final GeoIpService geoIpService;
    private final Supplier<PluginConfig> configSupplier;

    public ConnectionGuard(BypassStore bypassStore,
                           GeoIpService geoIpService,
                           Supplier<PluginConfig> configSupplier) {
        this.bypassStore = bypassStore;
        this.geoIpService = geoIpService;
        this.configSupplier = configSupplier;
    }

    public ConnectionDecision evaluate(UUID uuid, InetAddress address) {
        if (uuid != null && bypassStore.containsUuid(uuid)) {
            return ConnectionDecision.allow(LookupStatus.OK);
        }
        if (address != null && bypassStore.matchesIp(address)) {
            return ConnectionDecision.allow(LookupStatus.OK);
        }

        PluginConfig config = configSupplier.get();
        LookupResolution resolution = resolve(address);

        if (resolution.status() != LookupStatus.OK) {
            return config.onLookupFailure() == FailurePolicy.DENY
                    ? ConnectionDecision.deny(DenialReason.LOOKUP_FAILED, resolution.status())
                    : ConnectionDecision.allow(resolution.status());
        }

        LookupResult result = resolution.result().orElseThrow();
        String iso = result.countryIso();
        boolean proxy = result.proxy();

        if (proxy && config.vpnDetection().enabled() && config.vpnDetection().blockOnDetection()) {
            return ConnectionDecision.deny(DenialReason.VPN_DETECTED, iso, true);
        }

        boolean inList = config.countries().contains(iso);
        if (config.mode() == FilterMode.BLACKLIST && inList) {
            return ConnectionDecision.deny(DenialReason.COUNTRY_BLACKLISTED, iso, proxy);
        }
        if (config.mode() == FilterMode.WHITELIST && !inList) {
            return ConnectionDecision.deny(DenialReason.COUNTRY_NOT_WHITELISTED, iso, proxy);
        }

        return ConnectionDecision.allow(iso, proxy);
    }

    private LookupResolution resolve(InetAddress address) {
        if (address == null || isPrivate(address)) {
            return new LookupResolution(LookupStatus.PRIVATE, Optional.empty());
        }
        if (!geoIpService.isDatabaseLoaded()) {
            return new LookupResolution(LookupStatus.DB_MISSING, Optional.empty());
        }
        Optional<LookupResult> result = geoIpService.lookup(address);
        if (result.isEmpty()) {
            return new LookupResolution(LookupStatus.LOOKUP_FAILED, Optional.empty());
        }
        return new LookupResolution(LookupStatus.OK, result);
    }

    private static boolean isPrivate(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }

    private record LookupResolution(LookupStatus status, Optional<LookupResult> result) {
    }
}
