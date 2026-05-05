package fr.horizonsmp.geoBlock.geoip;

import java.io.Closeable;
import java.net.InetAddress;
import java.util.Optional;

public interface GeoIpService extends Closeable {

    /**
     * Resolves the country (and optionally the proxy flag) for the given
     * address. Returns an empty Optional when the address is private,
     * local, or not present in the underlying database.
     */
    Optional<LookupResult> lookup(InetAddress address);

    /**
     * Reloads the underlying database files. Implementations should
     * remain usable while a reload is in progress (best effort).
     */
    void reload();
}
