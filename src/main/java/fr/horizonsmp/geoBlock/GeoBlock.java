package fr.horizonsmp.geoBlock;

import fr.horizonsmp.geoBlock.bypass.BypassStore;
import fr.horizonsmp.geoBlock.config.ConfigLoader;
import fr.horizonsmp.geoBlock.config.PluginConfig;
import fr.horizonsmp.geoBlock.geoip.GeoIpService;
import fr.horizonsmp.geoBlock.geoip.MaxMindGeoIpService;
import fr.horizonsmp.geoBlock.geoip.MmdbAutoUpdater;
import fr.horizonsmp.geoBlock.i18n.Messages;
import fr.horizonsmp.geoBlock.listener.ConnectionGuard;
import fr.horizonsmp.geoBlock.listener.PreLoginListener;
import fr.horizonsmp.geoBlock.permission.PermissionService;
import org.bukkit.plugin.java.JavaPlugin;

public final class GeoBlock extends JavaPlugin {

    private ConfigLoader configLoader;
    private Messages messages;
    private PluginConfig config;
    private MaxMindGeoIpService geoIpService;
    private MmdbAutoUpdater autoUpdater;
    private BypassStore bypassStore;
    private ConnectionGuard connectionGuard;
    private PermissionService permissionService;

    @Override
    public void onEnable() {
        this.configLoader = new ConfigLoader(this);
        this.messages = new Messages(this);
        this.config = configLoader.load();
        this.messages.load();

        this.bypassStore = new BypassStore(this);
        this.bypassStore.load();

        this.geoIpService = new MaxMindGeoIpService(getLogger(), getDataFolder().toPath(), config);
        this.geoIpService.initialize();

        this.autoUpdater = new MmdbAutoUpdater(this, geoIpService);
        this.autoUpdater.start(config);

        this.connectionGuard = new ConnectionGuard(bypassStore, geoIpService, this::pluginConfig);

        this.permissionService = new PermissionService(this);
        this.permissionService.announceProvider();

        getServer().getPluginManager().registerEvents(
                new PreLoginListener(connectionGuard, messages), this);

        getLogger().info("GeoBlock loaded in " + config.mode() + " mode with "
                + config.countries().size() + " countries.");
    }

    @Override
    public void onDisable() {
        if (autoUpdater != null) {
            autoUpdater.stop();
        }
        if (geoIpService != null) {
            geoIpService.close();
        }
    }

    public void reloadAll() {
        this.config = configLoader.load();
        this.messages.load();
        this.bypassStore.load();
        if (geoIpService != null) {
            geoIpService.updateConfig(config);
            geoIpService.reload();
        }
        if (autoUpdater != null) {
            autoUpdater.start(config);
        }
    }

    public PluginConfig pluginConfig() {
        return config;
    }

    public Messages messages() {
        return messages;
    }

    public GeoIpService geoIpService() {
        return geoIpService;
    }

    public BypassStore bypassStore() {
        return bypassStore;
    }

    public ConnectionGuard connectionGuard() {
        return connectionGuard;
    }

    public PermissionService permissionService() {
        return permissionService;
    }
}
