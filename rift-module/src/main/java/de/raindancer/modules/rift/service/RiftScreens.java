package de.raindancer.modules.rift.service;

import de.raindancer.core.ui.hologram.ScreenKind;
import de.raindancer.core.ui.hologram.Screens;
import de.raindancer.modules.rift.rules.ScreenRule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.OptionalInt;

/**
 * The rift's screen for Core's {@code /screen}: everybody's own pages and how full their rift is. A rift is private,
 * so there is nothing to show everybody; the rift is only looked at while it is held in memory, never loaded for a
 * screen.
 */
public final class RiftScreens {

    public static final List<String> KINDS = List.of("rift-you");
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final PageService pages;
    private final RiftService rift;
    private final ScreenRule rule = new ScreenRule();

    public RiftScreens(PageService pages, RiftService rift) {
        this.pages = pages;
        this.rift = rift;
    }

    public void register(Screens screens, Plugin owner) {
        screens.register(owner, ScreenKind.personal("rift-you", "Your rift",
                "Everybody's own rift: its pages, how full it is and when the next free page comes.",
                Material.ENDER_CHEST, viewer -> MINI.deserialize(rule.you(pages.pages(viewer.id()),
                        rift.held(viewer.id()).map(stash -> OptionalInt.of(stash.size())).orElse(OptionalInt.empty())))));
    }
}
