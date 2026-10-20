package de.raindancer.modules.rift.rules;

import de.raindancer.core.content.items.ItemWords;
import de.raindancer.modules.rift.model.Filter;

/** Whether a stack is shown under a filter: of the kind, if one is chosen, and found by Core's word search. */
public final class ItemMatchRule implements IRiftRule {

    public boolean matches(ItemWords item, Filter filter) {
        if (filter.kind() != null && item.kind() != filter.kind()) {
            return false;
        }
        return !filter.isSearching() || item.matches(filter.query());
    }

    @Override
    public String describe() {
        return "whether a stack is shown under a search and a kind";
    }
}
