package fr.horizonsmp.geoBlock.command;

import fr.horizonsmp.geoBlock.GeoBlock;
import fr.horizonsmp.geoBlock.bypass.BypassStore;
import fr.horizonsmp.geoBlock.i18n.Messages;
import fr.horizonsmp.geoBlock.permission.PermissionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class GeoBlockCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT_SUBS = List.of("reload", "bypass", "help");
    private static final List<String> BYPASS_ACTIONS = List.of("add", "remove", "list");
    private static final List<String> BYPASS_TYPES = List.of("ip", "uuid");

    private final GeoBlock plugin;
    private final Messages messages;
    private final PermissionService permissions;

    public GeoBlockCommand(GeoBlock plugin) {
        this.plugin = plugin;
        this.messages = plugin.messages();
        this.permissions = plugin.permissionService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {
        if (!permissions.canUseAnyCommand(sender)) {
            sender.sendMessage(messages.get("command.no-permission"));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "bypass" -> handleBypass(sender, label, args);
            case "help" -> sendHelp(sender, label);
            default -> sender.sendMessage(messages.get("command.unknown-subcommand",
                    Map.of("label", label)));
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!permissions.canReload(sender)) {
            sender.sendMessage(messages.get("command.no-permission"));
            return;
        }
        try {
            plugin.reloadAll();
            sender.sendMessage(messages.get("command.reload-success"));
        } catch (RuntimeException e) {
            sender.sendMessage(messages.get("command.reload-failed",
                    Map.of("error", String.valueOf(e.getMessage()))));
            plugin.getLogger().warning("Reload failed: " + e.getMessage());
        }
    }

    private void handleBypass(CommandSender sender, String label, String[] args) {
        if (!permissions.canManageBypass(sender)) {
            sender.sendMessage(messages.get("command.no-permission"));
            return;
        }
        if (args.length < 2) {
            sendHelp(sender, label);
            return;
        }
        BypassStore store = plugin.bypassStore();
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "add" -> handleBypassMutation(sender, store, args, true);
            case "remove" -> handleBypassMutation(sender, store, args, false);
            case "list" -> sendBypassList(sender, store);
            default -> sender.sendMessage(messages.get("command.unknown-subcommand",
                    Map.of("label", label)));
        }
    }

    private void handleBypassMutation(CommandSender sender,
                                      BypassStore store,
                                      String[] args,
                                      boolean adding) {
        if (args.length < 4) {
            sendHelp(sender, "geoblock");
            return;
        }
        String type = args[2].toLowerCase(Locale.ROOT);
        String value = args[3];
        Map<String, String> placeholders = Map.of("value", value);

        switch (type) {
            case "ip" -> {
                if (adding) {
                    BypassStore.AddResult result = store.addIp(value);
                    sender.sendMessage(messages.get(switch (result) {
                        case ADDED -> "command.bypass.ip-added";
                        case ALREADY_PRESENT -> "command.bypass.ip-already-listed";
                        case INVALID -> "command.bypass.invalid-ip";
                    }, placeholders));
                } else {
                    BypassStore.RemoveResult result = store.removeIp(value);
                    sender.sendMessage(messages.get(switch (result) {
                        case REMOVED -> "command.bypass.ip-removed";
                        case NOT_FOUND -> "command.bypass.ip-not-found";
                        case INVALID -> "command.bypass.invalid-ip";
                    }, placeholders));
                }
            }
            case "uuid" -> {
                if (adding) {
                    BypassStore.AddResult result = store.addUuid(value);
                    sender.sendMessage(messages.get(switch (result) {
                        case ADDED -> "command.bypass.uuid-added";
                        case ALREADY_PRESENT -> "command.bypass.uuid-already-listed";
                        case INVALID -> "command.bypass.invalid-uuid";
                    }, placeholders));
                } else {
                    BypassStore.RemoveResult result = store.removeUuid(value);
                    sender.sendMessage(messages.get(switch (result) {
                        case REMOVED -> "command.bypass.uuid-removed";
                        case NOT_FOUND -> "command.bypass.uuid-not-found";
                        case INVALID -> "command.bypass.invalid-uuid";
                    }, placeholders));
                }
            }
            default -> sender.sendMessage(messages.get("command.unknown-subcommand",
                    Map.of("label", "geoblock")));
        }
    }

    private void sendBypassList(CommandSender sender, BypassStore store) {
        sender.sendMessage(messages.get("command.bypass.list-header"));
        List<String> ips = store.listIps();
        List<UUID> uuids = store.listUuids();
        if (ips.isEmpty() && uuids.isEmpty()) {
            sender.sendMessage(messages.get("command.bypass.list-empty"));
            return;
        }
        for (String ip : ips) {
            sender.sendMessage(messages.get("command.bypass.list-ip-entry",
                    Map.of("value", ip)));
        }
        for (UUID uuid : uuids) {
            sender.sendMessage(messages.get("command.bypass.list-uuid-entry",
                    Map.of("value", uuid.toString())));
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        Map<String, String> placeholders = Map.of("label", label);
        sender.sendMessage(messages.get("command.help.header"));
        sender.sendMessage(messages.get("command.help.reload", placeholders));
        sender.sendMessage(messages.get("command.help.bypass-add-ip", placeholders));
        sender.sendMessage(messages.get("command.help.bypass-add-uuid", placeholders));
        sender.sendMessage(messages.get("command.help.bypass-remove-ip", placeholders));
        sender.sendMessage(messages.get("command.help.bypass-remove-uuid", placeholders));
        sender.sendMessage(messages.get("command.help.bypass-list", placeholders));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {
        if (!permissions.canUseAnyCommand(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            return startsWith(ROOT_SUBS, args[0]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("bypass")) {
            if (args.length == 2) {
                return startsWith(BYPASS_ACTIONS, args[1]);
            }
            if (args.length == 3 && (args[1].equalsIgnoreCase("add")
                    || args[1].equalsIgnoreCase("remove"))) {
                return startsWith(BYPASS_TYPES, args[2]);
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("remove")) {
                BypassStore store = plugin.bypassStore();
                if (args[2].equalsIgnoreCase("ip")) {
                    return startsWith(store.listIps(), args[3]);
                }
                if (args[2].equalsIgnoreCase("uuid")) {
                    List<String> uuids = new ArrayList<>(store.listUuids().size());
                    for (UUID uuid : store.listUuids()) {
                        uuids.add(uuid.toString());
                    }
                    return startsWith(uuids, args[3]);
                }
            }
        }
        return List.of();
    }

    private static List<String> startsWith(List<String> source, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : source) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(value);
            }
        }
        return out;
    }
}
