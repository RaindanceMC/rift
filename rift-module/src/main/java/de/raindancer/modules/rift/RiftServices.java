package de.raindancer.modules.rift;

import de.raindancer.core.RainsCore;
import de.raindancer.core.data.settings.SettingsStore;
import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.core.platform.util.Cooldowns;
import de.raindancer.core.ui.chat.Brand;
import de.raindancer.core.ui.effect.Effects;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.rift.service.PageService;
import de.raindancer.modules.rift.service.RiftService;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.function.Supplier;

/** Everything this module has built, in one place — a record: it owns nothing, it only carries. */
public record RiftServices(
        Plugin plugin,
        Server server,
        LogChannel log,
        Messages messages,
        Brand brand,
        RainsCore core,

        Supplier<RiftSettings> settings,
        SettingsStore<RiftSettings> store,

        RiftService rift,
        PageService pages,
        Cooldowns<UUID> sellWait,
        IRiftScreensOpener screens) {

    public RiftSettings config() {
        return settings.get();
    }

    public Effects effects() {
        return core.effects();
    }
}
