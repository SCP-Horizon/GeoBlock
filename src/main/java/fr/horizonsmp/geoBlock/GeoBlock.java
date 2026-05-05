package fr.horizonsmp.geoBlock;

import fr.horizonsmp.geoBlock.config.ConfigLoader;
import fr.horizonsmp.geoBlock.config.PluginConfig;
import fr.horizonsmp.geoBlock.i18n.Messages;
import org.bukkit.plugin.java.JavaPlugin;

public final class GeoBlock extends JavaPlugin {

    private ConfigLoader configLoader;
    private Messages messages;
    private PluginConfig config;

    @Override
    public void onEnable() {
        this.configLoader = new ConfigLoader(this);
        this.messages = new Messages(this);
        reloadAll();
        getLogger().info("GeoBlock loaded in " + config.mode() + " mode with "
                + config.countries().size() + " countries.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void reloadAll() {
        this.config = configLoader.load();
        this.messages.load();
    }

    public PluginConfig pluginConfig() {
        return config;
    }

    public Messages messages() {
        return messages;
    }
}
