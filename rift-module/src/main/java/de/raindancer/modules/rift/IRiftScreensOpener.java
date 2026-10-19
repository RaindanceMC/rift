package de.raindancer.modules.rift;

import org.bukkit.entity.Player;

/** Opening this module's screens, without the command — built at bootstrap — knowing a menu class. */
public interface IRiftScreensOpener {

    /** The rift itself; {@code search} narrows it, null or blank for everything. */
    void rift(Player viewer, String search);
}
