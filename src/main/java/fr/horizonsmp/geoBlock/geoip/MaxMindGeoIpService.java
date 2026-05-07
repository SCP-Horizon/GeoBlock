package fr.horizonsmp.geoBlock.geoip;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
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
    private final AtomicReference<DatabaseReader> readerRef = new AtomicReference<>();
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
    public boolean isDatabaseLoaded() {
        return readerRef.get() != null;
    }

    @Override
    public Optional<LookupResult> lookup(InetAddress address) {
        if (address == null || isPrivate(address)) {
            return NO_RESULT;
        }
        return cache.get(address, this::resolve);
    }

    private Optional<LookupResult> resolve(InetAddress address) {
        DatabaseReader reader = readerRef.get();
        if (reader == null) {
            return NO_RESULT;
        }
        try {
            CountryResponse country = reader.country(address);
            String iso = country.getCountry() != null ? country.getCountry().getIsoCode() : null;
            if (iso == null || iso.isBlank()) {
                return NO_RESULT;
            }
            return Optional.of(new LookupResult(iso));
        } catch (AddressNotFoundException e) {
            return NO_RESULT;
        } catch (IOException | GeoIp2Exception e) {
            logger.log(Level.WARNING, "GeoIP lookup failed for " + address.getHostAddress(), e);
            return NO_RESULT;
        }
    }

    @Override
    public synchronized void reload() {
        PluginConfig cfg = configRef.get();
        DatabaseReader next = openReader(resolvePath(cfg.geoip().databasePath()));
        DatabaseReader previous = readerRef.getAndSet(next);
        cache.invalidateAll();
        closeQuietly(previous);

        if (next == null) {
            logger.warning("GeoIP country database is not loaded. Connections will follow the on-lookup-failure policy.");
        } else {
            logger.info("GeoIP country database loaded from " + cfg.geoip().databasePath());
        }
    }

    private DatabaseReader openReader(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return new DatabaseReader.Builder(path.toFile())
                    .fileMode(com.maxmind.db.Reader.FileMode.MEMORY_MAPPED)
                    .build();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to open country database at " + path, e);
            return null;
        }
    }

    private Path resolvePath(String configured) {
        Path direct = Path.of(configured);
        return direct.isAbsolute() ? direct : dataFolder.resolve(configured);
    }

    @Override
    public void close() {
        DatabaseReader previous = readerRef.getAndSet(null);
        closeQuietly(previous);
    }

    private void closeQuietly(DatabaseReader reader) {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (IOException e) {
            logger.log(Level.FINE, "Error closing country reader", e);
        }
    }

    private static boolean isPrivate(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }
}
