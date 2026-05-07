package fr.horizonsmp.geoBlock.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ConfigLoader {

    private final JavaPlugin plugin;

    public ConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public PluginConfig load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        FilterMode mode = FilterMode.fromString(cfg.getString("mode"), FilterMode.BLACKLIST);
        Set<String> countries = parseCountries(cfg.getStringList("countries"));
        FailurePolicy onLookupFailure = FailurePolicy.fromString(
                cfg.getString("on-lookup-failure"), FailurePolicy.ALLOW);

        ConfigurationSection geoSection = section(cfg, "geoip");
        PluginConfig.GeoIp geoip = new PluginConfig.GeoIp(
                GeoIpProvider.fromString(geoSection.getString("provider"), GeoIpProvider.DB_IP),
                geoSection.getString("license-key", ""),
                geoSection.getBoolean("auto-update", true),
                geoSection.getLong("update-interval-hours", 168L),
                geoSection.getString("database-path", "country.mmdb")
        );

        ConfigurationSection discordSection = section(cfg, "discord");
        PluginConfig.Discord discord = new PluginConfig.Discord(
                discordSection.getString("webhook-url", ""),
                discordSection.getBoolean("include-ip", true),
                discordSection.getString("username", "GeoBlock"),
                discordSection.getBoolean("notify-lookup-failure", true)
        );

        return new PluginConfig(mode, countries, onLookupFailure, geoip, discord);
    }

    private static Set<String> parseCountries(List<String> raw) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String entry : raw) {
            if (entry == null) {
                continue;
            }
            String trimmed = entry.trim().toUpperCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return Set.copyOf(normalized);
    }

    private static ConfigurationSection section(FileConfiguration cfg, String path) {
        ConfigurationSection s = cfg.getConfigurationSection(path);
        return s != null ? s : cfg.createSection(path);
    }
}
