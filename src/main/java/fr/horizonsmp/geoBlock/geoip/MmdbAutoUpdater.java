package fr.horizonsmp.geoBlock.geoip;

import fr.horizonsmp.geoBlock.config.PluginConfig;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MmdbAutoUpdater {

    private static final String COUNTRY_EDITION = "GeoLite2-Country";
    private static final String ANONYMOUS_EDITION = "GeoLite2-Anonymous-IP";
    private static final long TICKS_PER_SECOND = 20L;

    private final Plugin plugin;
    private final MaxMindGeoIpService geoIpService;
    private final MmdbDownloader downloader;
    private final Logger logger;

    private BukkitTask task;

    public MmdbAutoUpdater(Plugin plugin, MaxMindGeoIpService geoIpService) {
        this.plugin = plugin;
        this.geoIpService = geoIpService;
        this.downloader = new MmdbDownloader();
        this.logger = plugin.getLogger();
    }

    public void start(PluginConfig config) {
        stop();
        if (!config.geoip().autoUpdate()) {
            return;
        }
        if (config.geoip().licenseKey().isBlank()) {
            logger.info("GeoIP auto-update is enabled but no license key is set. Skipping downloads.");
            return;
        }

        long intervalSeconds = Math.max(1L, config.geoip().updateIntervalHours()) * TimeUnit.HOURS.toSeconds(1);
        long intervalTicks = intervalSeconds * TICKS_PER_SECOND;

        this.task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> updateAll(config),
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

    private void updateAll(PluginConfig config) {
        boolean anyChange = false;
        anyChange |= downloadIfNeeded(COUNTRY_EDITION, config.geoip().licenseKey(),
                resolvePath(config.geoip().databasePath()));
        if (config.vpnDetection().enabled()) {
            anyChange |= downloadIfNeeded(ANONYMOUS_EDITION, config.geoip().licenseKey(),
                    resolvePath(config.vpnDetection().databasePath()));
        }
        if (anyChange) {
            geoIpService.reload();
        }
    }

    private boolean downloadIfNeeded(String edition, String licenseKey, Path target) {
        try {
            Files.createDirectories(target.getParent());
            logger.info("Downloading " + edition + " from MaxMind ...");
            downloader.download(edition, licenseKey, target);
            logger.info("Updated " + edition + " at " + target);
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to update " + edition + ": " + e.getMessage());
            return false;
        }
    }

    private Path resolvePath(String configured) {
        Path direct = Path.of(configured);
        return direct.isAbsolute() ? direct : plugin.getDataFolder().toPath().resolve(configured);
    }
}
