package de.raindancer.modules.rift.listener;

import de.raindancer.modules.rift.RiftServices;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** A rift is held in memory while its owner is around: their pages read at join, written and let go at quit. */
public final class RiftPlayerListener implements IRiftListener {

    private final RiftServices services;

    public RiftPlayerListener(RiftServices services) {
        this.services = services;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        services.pages().noticed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        services.rift().forget(event.getPlayer().getUniqueId());
        services.sellWait().forget(event.getPlayer().getUniqueId());
    }
}
