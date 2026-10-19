package de.raindancer.modules.rift.command;

import de.raindancer.modules.rift.RiftServices;
import de.raindancer.modules.rift.util.PermissionNodes;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * {@code /rift} opens it, {@code /rift <words>} opens it searching, {@code /rift sort} sorts it in the player's own
 * order without opening anything.
 */
public final class RiftCommand implements IRiftCommand {

    private final Supplier<RiftServices> services;

    public RiftCommand(Supplier<RiftServices> services) {
        this.services = services;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
        RiftServices live = services.get();
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            live.messages().send(sender, "rift.only-a-player");
            return;
        }
        if (!player.hasPermission(PermissionNodes.USE)) {
            live.messages().send(player, "rift.not-allowed");
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("sort")) {
            live.rift().resize(player.getUniqueId());
            live.rift().sort(player, live.rift().prefs(player.getUniqueId()).sort());
            return;
        }
        live.screens().rift(player, String.join(" ", args));
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, String @NotNull [] args) {
        if (args.length <= 1) {
            String started = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return "sort".startsWith(started) ? List.of("sort") : List.of();
        }
        return List.of();
    }

    @Override
    public String describe() {
        return "opening your rift, searching it, sorting it";
    }
}
