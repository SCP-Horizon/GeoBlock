package fr.horizonsmp.geoBlock.permission;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public final class PermissionService {

    public static final String ADMIN = "geoblock.admin";
    public static final String COMMAND_RELOAD = "geoblock.command.reload";
    public static final String COMMAND_BYPASS = "geoblock.command.bypass";

    private final Logger logger;

    public PermissionService(Plugin plugin) {
        this.logger = plugin.getLogger();
    }

    public void announceProvider() {
        Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (luckPerms != null && luckPerms.isEnabled()) {
            logger.info("LuckPerms detected; permissions will be served through it.");
        } else {
            logger.info("LuckPerms not detected; permissions fall back to operator status.");
        }
    }

    public boolean canReload(CommandSender sender) {
        return sender.hasPermission(COMMAND_RELOAD) || sender.hasPermission(ADMIN);
    }

    public boolean canManageBypass(CommandSender sender) {
        return sender.hasPermission(COMMAND_BYPASS) || sender.hasPermission(ADMIN);
    }

    public boolean canUseAnyCommand(CommandSender sender) {
        return canReload(sender) || canManageBypass(sender);
    }
}
