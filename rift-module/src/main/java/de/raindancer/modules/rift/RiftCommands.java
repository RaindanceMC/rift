package de.raindancer.modules.rift;

import de.raindancer.modules.api.ModuleCommand;
import de.raindancer.modules.rift.command.RiftCommand;

import java.util.List;

/**
 * What this module declares at bootstrap, and how it is filled in later. Paper fires {@code COMMANDS} during
 * bootstrap, long before the module is enabled, so the handler holds a supplier and the state to design for is
 * <em>registered, module not running</em>.
 */
public final class RiftCommands {

    private static volatile RiftServices services;

    private RiftCommands() {
    }

    public static List<ModuleCommand> declared() {
        return List.of(
                ModuleCommand.of("rift", "Open your rift — your own pocket dimension for your things",
                                new RiftCommand(RiftCommands::require))
                        .aliased("pv", "playervault")
                        .taking("[search words]")
                        .taking("sort"));
    }

    static void ready(RiftServices live) {
        services = live;
    }

    static void stopped() {
        services = null;
    }

    public static boolean isRunning() {
        return services != null;
    }

    private static RiftServices require() {
        RiftServices live = services;
        if (live == null) {
            throw new IllegalStateException("the rift module is not running");
        }
        return live;
    }
}
