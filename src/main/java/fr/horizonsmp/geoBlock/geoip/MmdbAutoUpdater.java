package fr.horizonsmp.geoBlock.geoip;

import fr.horizonsmp.geoBlock.config.GeoIpProvider;
import fr.horizonsmp.geoBlock.config.PluginConfig;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MmdbAutoUpdater {

    private static final String COUNTRY_EDITION_MAXMIND = "GeoLite2-Country";
    private static final long TICKS_PER_SECOND = 20L;

    private final Plugin plugin;
    private final MaxMindGeoIpService geoIpService;
    private final MmdbDownloader downloader;
    private final Logger logger;
    private final Supplier<PluginConfig> configSupplier;

    private BukkitTask task;

    public MmdbAutoUpdater(Plugin plugin,
                           MaxMindGeoIpService geoIpService,
                           Supplier<PluginConfig> configSupplier) {
        this.plugin = plugin;
        this.geoIpService = geoIpService;
        this.downloader = new MmdbDownloader();
        this.logger = plugin.getLogger();
        this.configSupplier = configSupplier;
    }

    public void start() {
        stop();
        PluginConfig config = configSupplier.get();
        if (!config.geoip().autoUpdate()) {
            return;
        }
        if (requiresLicense(config) && config.geoip().licenseKey().isBlank()) {
            logger.info("MaxMind provider selected but no license key is set. Skipping downloads.");
            return;
        }

        long intervalSeconds = Math.max(1L, config.geoip().updateIntervalHours()) * TimeUnit.HOURS.toSeconds(1);
        long intervalTicks = intervalSeconds * TICKS_PER_SECOND;

        this.task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::runScheduledUpdate,
                0L,
                intervalTicks
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * Triggers an immediate update on a background thread, regardless of
     * the auto-update schedule. Resolves with NO_LICENSE when MaxMind is
     * selected without a license key.
     */
    public CompletableFuture<UpdateOutcome> triggerUpdateNow() {
        PluginConfig config = configSupplier.get();
        if (requiresLicense(config) && config.geoip().licenseKey().isBlank()) {
            return CompletableFuture.completedFuture(UpdateOutcome.NO_LICENSE);
        }
        return CompletableFuture.supplyAsync(() -> performUpdate(config));
    }

    private static boolean requiresLicense(PluginConfig config) {
        return config.geoip().provider() == GeoIpProvider.MAXMIND;
    }

    private void runScheduledUpdate() {
        performUpdate(configSupplier.get());
    }

    private UpdateOutcome performUpdate(PluginConfig config) {
        boolean countrySuccess = downloadCountry(config);
        if (countrySuccess) {
            geoIpService.reload();
        }
        return countrySuccess ? UpdateOutcome.SUCCESS : UpdateOutcome.FAILED;
    }

    private boolean downloadCountry(PluginConfig config) {
        Path target = resolvePath(config.geoip().databasePath());
        return switch (config.geoip().provider()) {
            case DB_IP -> downloadDbIp(target);
            case MAXMIND -> downloadMaxMindEdition(target,
                    config.geoip().licenseKey());
        };
    }

    private boolean downloadDbIp(Path target) {
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            logger.info("Downloading country database from db-ip.com ...");
            downloader.downloadDbIpCountry(target);
            logger.info("Updated country database at " + target);
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to update db-ip country database: " + e.getMessage());
            return false;
        }
    }

    private boolean downloadMaxMindEdition(Path target, String licenseKey) {
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            logger.info("Downloading " + COUNTRY_EDITION_MAXMIND + " from MaxMind ...");
            downloader.downloadMaxMind(COUNTRY_EDITION_MAXMIND, licenseKey, target);
            logger.info("Updated " + COUNTRY_EDITION_MAXMIND + " at " + target);
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to update " + COUNTRY_EDITION_MAXMIND + ": " + e.getMessage());
            return false;
        }
    }

    private Path resolvePath(String configured) {
        Path direct = Path.of(configured);
        return direct.isAbsolute() ? direct : plugin.getDataFolder().toPath().resolve(configured);
    }
}
