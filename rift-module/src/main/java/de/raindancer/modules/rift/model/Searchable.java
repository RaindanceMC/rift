package de.raindancer.modules.rift.model;

import de.raindancer.core.ui.choose.Category;

import java.util.List;

/**
 * What a search can find in one stack.
 *
 * @param material   the material, as a player reads it ("Oak Log")
 * @param customName the name it was given, or null
 * @param extra      everything else worth finding: enchantments, a potion's effect, the lore
 */
public record Searchable(String material, String customName, List<String> extra, Category kind) {

    public Searchable {
        extra = extra == null ? List.of() : List.copyOf(extra);
    }
}
