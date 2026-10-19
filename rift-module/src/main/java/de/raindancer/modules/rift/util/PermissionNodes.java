package de.raindancer.modules.rift.util;

import org.bukkit.Server;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.List;

/**
 * What this module asks about somebody. Registered programmatically, so a module hosted inside another plugin still
 * has its nodes — an unregistered node resolves to "operators only", which would lock every ordinary player out of
 * their own rift.
 */
public final class PermissionNodes {

    /** Opening your own rift. Everybody, by default: it is the whole point. */
    public static final String USE = "rains.rift.use";

    /** Selling from it. */
    public static final String SELL = "rains.rift.sell";

    private static final String BONUS = "rains.rift.pages.";

    /** The most bonus pages a node is registered for; higher ones still work, they are only not listed. */
    public static final int REGISTERED_BONUS = 20;

    private PermissionNodes() {
    }

    /** {@code rains.rift.pages.<n>}: n extra pages on top of what was earned and bought. */
    public static String bonusPages(int pages) {
        return BONUS + pages;
    }

    public static List<Permission> declared() {
        List<Permission> nodes = new ArrayList<>(List.of(
                new Permission(USE, "Open your own rift", PermissionDefault.TRUE),
                new Permission(SELL, "Sell from your rift to the shop", PermissionDefault.TRUE)));
        for (int pages = 1; pages <= REGISTERED_BONUS; pages++) {
            nodes.add(new Permission(bonusPages(pages), pages + " extra rift page(s)", PermissionDefault.FALSE));
        }
        return nodes;
    }

    /** @return how many were added, for the line in the log */
    public static int register(Server server) {
        if (server == null) {
            return 0;
        }
        int added = 0;
        for (Permission permission : declared()) {
            if (server.getPluginManager().getPermission(permission.getName()) != null) {
                continue;
            }
            try {
                server.getPluginManager().addPermission(permission);
                added++;
            } catch (IllegalArgumentException alreadyThere) {
                // Registered by another copy between the check and the add.
            }
        }
        return added;
    }
}
