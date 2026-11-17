package de.raindancer.modules.rift;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.rift.model.Prefs;
import de.raindancer.modules.rift.model.SortMode;
import de.raindancer.modules.rift.service.RiftService;
import de.raindancer.modules.rift.util.PermissionNodes;

import java.util.Arrays;
import java.util.List;

/** What Rift offers to Core's {@code /preferences}: the same two settings the sort button in the rift changes. */
final class RiftOffers {

    private static final List<SortMode> MODES = List.of(SortMode.values());

    private RiftOffers() {
    }

    static Preference sort(RiftService rift) {
        return Preference.choice("rift:sort", "Rift", "Sort your rift by", "How the things in your rift are put in order.",
                        Arrays.stream(SortMode.values()).map(SortMode::label).toList(),
                        player -> rift.prefs(player.getUniqueId()).sort().ordinal(),
                        (player, index) -> {
                            Prefs prefs = rift.prefs(player.getUniqueId());
                            SortMode mode = MODES.get(index);
                            rift.prefs(player.getUniqueId(), prefs.withSort(mode));
                            if (prefs.autoSort()) {
                                rift.sort(player, mode);
                            }
                        })
                .needing(PermissionNodes.USE);
    }

    static Preference autoSort(RiftService rift) {
        return Preference.toggle("rift:auto-sort", "Rift", "Sort your rift on its own",
                        "Keeps your rift in order every time you put something in.",
                        player -> rift.prefs(player.getUniqueId()).autoSort(),
                        (player, on) -> {
                            Prefs prefs = rift.prefs(player.getUniqueId());
                            rift.prefs(player.getUniqueId(), prefs.withAutoSort(on));
                            if (on && !prefs.autoSort()) {
                                rift.sort(player, prefs.sort());
                            }
                        })
                .needing(PermissionNodes.USE);
    }
}
