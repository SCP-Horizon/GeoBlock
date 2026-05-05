package fr.horizonsmp.geoBlock.geoip;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AnonymousIpResponse;
import com.maxmind.geoip2.model.CountryResponse;
import fr.horizonsmp.geoBlock.config.PluginConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MaxMindGeoIpService implements GeoIpService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final long CACHE_MAX_SIZE = 10_000L;
    private static final Optional<LookupResult> NO_RESULT = Optional.empty();

    private final Logger logger;
    private final Path dataFolder;
    private final AtomicReference<PluginConfig> configRef;
    private final AtomicReference<Readers> readersRef = new AtomicReference<>(Readers.EMPTY);
    private final Cache<InetAddress, Optional<LookupResult>> cache = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_TTL)
            .maximumSize(CACHE_MAX_SIZE)
            .build();

    public MaxMindGeoIpService(Logger logger, Path dataFolder, PluginConfig initialConfig) {
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.configRef = new AtomicReference<>(initialConfig);
    }

    public void initialize() {
        reload();
    }

    public void updateConfig(PluginConfig config) {
        this.configRef.set(config);
    }

    @Override
    public Optional<LookupResult> lookup(InetAddress address) {
        if (address == null || isPrivate(address)) {
            return NO_RESULT;
        }
        return cache.get(address, this::resolve);
    }

    private Optional<LookupResult> resolve(InetAddress address) {
        Readers readers = readersRef.get();
        if (readers.country == null) {
            return NO_RESULT;
        }
        try {
            CountryResponse country = readers.country.country(address);
            String iso = country.getCountry() != null ? country.getCountry().getIsoCode() : null;
            if (iso == null || iso.isBlank()) {
                return NO_RESULT;
            }
            LookupResult result = LookupResult.country(iso);
            if (readers.anonymous != null) {
                result = result.withProxy(detectProxy(readers.anonymous, address));
            }
            return Optional.of(result);
        } catch (AddressNotFoundException e) {
            return NO_RESULT;
        } catch (IOException | GeoIp2Exception e) {
            logger.log(Level.WARNING, "GeoIP lookup failed for " + address.getHostAddress(), e);
            return NO_RESULT;
        }
    }

    private static boolean detectProxy(DatabaseReader reader, InetAddress address) {
        try {
            AnonymousIpResponse r = reader.anonymousIp(address);
            return r.isAnonymous()
                    || r.isAnonymousVpn()
                    || r.isHostingProvider()
                    || r.isPublicProxy()
                    || r.isResidentialProxy()
                    || r.isTorExitNode();
        } catch (AddressNotFoundException e) {
            return false;
        } catch (IOException | GeoIp2Exception e) {
            return false;
        }
    }

    @Override
    public synchronized void reload() {
        PluginConfig cfg = configRef.get();
        Readers next = open(cfg);
        Readers previous = readersRef.getAndSet(next);
        cache.invalidateAll();
        closeQuietly(previous);

        if (next.country == null) {
            logger.warning("GeoIP country database is not loaded. Connections will follow the on-lookup-failure policy.");
        } else {
            logger.info("GeoIP country database loaded from " + cfg.geoip().databasePath());
        }
        if (cfg.vpnDetection().enabled()) {
            if (next.anonymous == null) {
                logger.warning("VPN detection enabled but anonymous-IP database is missing.");
            } else {
                logger.info("Anonymous-IP database loaded from " + cfg.vpnDetection().databasePath());
            }
        }
    }

    private Readers open(PluginConfig cfg) {
        DatabaseReader country = openReader(resolvePath(cfg.geoip().databasePath()), "country");
        DatabaseReader anonymous = null;
        if (cfg.vpnDetection().enabled()) {
            anonymous = openReader(resolvePath(cfg.vpnDetection().databasePath()), "anonymous-IP");
        }
        return new Readers(country, anonymous);
    }

    private DatabaseReader openReader(Path path, String label) {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return new DatabaseReader.Builder(path.toFile())
                    .fileMode(com.maxmind.db.Reader.FileMode.MEMORY_MAPPED)
                    .build();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to open " + label + " database at " + path, e);
            return null;
        }
    }

    private Path resolvePath(String configured) {
        Path direct = Path.of(configured);
        return direct.isAbsolute() ? direct : dataFolder.resolve(configured);
    }

    @Override
    public void close() {
        Readers previous = readersRef.getAndSet(Readers.EMPTY);
        closeQuietly(previous);
    }

    private void closeQuietly(Readers readers) {
        if (readers == null) {
            return;
        }
        if (readers.country != null) {
            try {
                readers.country.close();
            } catch (IOException e) {
                logger.log(Level.FINE, "Error closing country reader", e);
            }
        }
        if (readers.anonymous != null) {
            try {
                readers.anonymous.close();
            } catch (IOException e) {
                logger.log(Level.FINE, "Error closing anonymous reader", e);
            }
        }
    }

    private static boolean isPrivate(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }

    private record Readers(DatabaseReader country, DatabaseReader anonymous) {
        static final Readers EMPTY = new Readers(null, null);
    }
}
