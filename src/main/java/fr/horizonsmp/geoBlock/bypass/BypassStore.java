package fr.horizonsmp.geoBlock.bypass;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BypassStore {

    private static final String FILE_NAME = "whitelist.yml";
    private static final String IPS_KEY = "ips";
    private static final String UUIDS_KEY = "uuids";

    public enum AddResult {ADDED, ALREADY_PRESENT, INVALID}

    public enum RemoveResult {REMOVED, NOT_FOUND, INVALID}

    private final Set<UUID> uuids = ConcurrentHashMap.newKeySet();
    private final List<IpEntry> ips = new CopyOnWriteArrayList<>();

    private final JavaPlugin plugin;
    private final File file;
    private final Logger logger;

    public BypassStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        this.logger = plugin.getLogger();
    }

    public synchronized void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            logger.warning("Failed to create plugin data folder for bypass store.");
        }
        if (!file.exists()) {
            save();
            return;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        uuids.clear();
        for (String raw : cfg.getStringList(UUIDS_KEY)) {
            try {
                uuids.add(UUID.fromString(raw.trim()));
            } catch (IllegalArgumentException e) {
                logger.warning("Skipped invalid UUID in bypass list: " + raw);
            }
        }
        ips.clear();
        for (String raw : cfg.getStringList(IPS_KEY)) {
            IpMatcher.parse(raw).ifPresentOrElse(
                    matcher -> ips.add(new IpEntry(normalize(raw), matcher)),
                    () -> logger.warning("Skipped invalid IP in bypass list: " + raw));
        }
    }

    public synchronized void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        List<String> uuidList = new ArrayList<>();
        for (UUID uuid : uuids) {
            uuidList.add(uuid.toString());
        }
        uuidList.sort(Comparator.naturalOrder());
        cfg.set(UUIDS_KEY, uuidList);

        List<String> ipList = new ArrayList<>();
        for (IpEntry entry : ips) {
            ipList.add(entry.raw());
        }
        ipList.sort(Comparator.naturalOrder());
        cfg.set(IPS_KEY, ipList);

        try {
            cfg.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save bypass list to " + file, e);
        }
    }

    public boolean containsUuid(UUID uuid) {
        return uuid != null && uuids.contains(uuid);
    }

    public boolean matchesIp(InetAddress address) {
        if (address == null) {
            return false;
        }
        for (IpEntry entry : ips) {
            if (entry.matcher().matches(address)) {
                return true;
            }
        }
        return false;
    }

    public AddResult addUuid(String raw) {
        UUID uuid;
        try {
            uuid = UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return AddResult.INVALID;
        }
        boolean added = uuids.add(uuid);
        if (added) {
            save();
        }
        return added ? AddResult.ADDED : AddResult.ALREADY_PRESENT;
    }

    public RemoveResult removeUuid(String raw) {
        UUID uuid;
        try {
            uuid = UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return RemoveResult.INVALID;
        }
        boolean removed = uuids.remove(uuid);
        if (removed) {
            save();
        }
        return removed ? RemoveResult.REMOVED : RemoveResult.NOT_FOUND;
    }

    public AddResult addIp(String raw) {
        Optional<IpMatcher> matcher = IpMatcher.parse(raw);
        if (matcher.isEmpty()) {
            return AddResult.INVALID;
        }
        String normalized = normalize(raw);
        for (IpEntry entry : ips) {
            if (entry.raw().equalsIgnoreCase(normalized)) {
                return AddResult.ALREADY_PRESENT;
            }
        }
        ips.add(new IpEntry(normalized, matcher.get()));
        save();
        return AddResult.ADDED;
    }

    public RemoveResult removeIp(String raw) {
        if (raw == null || raw.isBlank()) {
            return RemoveResult.INVALID;
        }
        String normalized = normalize(raw);
        Optional<IpEntry> match = ips.stream()
                .filter(e -> e.raw().equalsIgnoreCase(normalized))
                .findFirst();
        if (match.isEmpty()) {
            return RemoveResult.NOT_FOUND;
        }
        ips.remove(match.get());
        save();
        return RemoveResult.REMOVED;
    }

    public List<String> listIps() {
        List<String> out = new ArrayList<>(ips.size());
        for (IpEntry entry : ips) {
            out.add(entry.raw());
        }
        out.sort(Comparator.naturalOrder());
        return List.copyOf(out);
    }

    public List<UUID> listUuids() {
        List<UUID> out = new ArrayList<>(uuids);
        out.sort(Comparator.naturalOrder());
        return List.copyOf(out);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private record IpEntry(String raw, IpMatcher matcher) {
    }
}
