package fr.horizonsmp.geoBlock.geoip;

import java.io.Closeable;
import java.net.InetAddress;
import java.util.Optional;

public interface GeoIpService extends Closeable {

    /**
     * Resolves the country for the given address. Returns an empty
     * Optional when the address is private, local, or not present in
     * the underlying database.
     */
    Optional<LookupResult> lookup(InetAddress address);

    /**
     * Whether the country database is currently loaded and ready to
     * serve lookups. Used by callers to distinguish a missing database
     * from an address that simply was not found.
     */
    boolean isDatabaseLoaded();

    /**
     * Reloads the underlying database files. Implementations should
     * remain usable while a reload is in progress (best effort).
     */
    void reload();
}
