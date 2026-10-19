package de.raindancer.modules.rift.rules;

import de.raindancer.modules.rift.model.Filter;
import de.raindancer.modules.rift.model.Searchable;

import java.util.Locale;

/** Whether a stack is shown under a filter: of the kind, if one is chosen, and with every searched word in it. */
public final class ItemMatchRule implements IRiftRule {

    public boolean matches(Searchable item, Filter filter) {
        if (filter.kind() != null && item.kind() != filter.kind()) {
            return false;
        }
        if (!filter.isSearching()) {
            return true;
        }
        String text = normal(String.join(" ", item.material(),
                item.customName() == null ? "" : item.customName(), String.join(" ", item.extra())));
        for (String word : normal(filter.query()).split(" ")) {
            if (!word.isEmpty() && !text.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /** Lower case, underscores as spaces, so "oak_log" finds "Oak Log". */
    private static String normal(String text) {
        return text.toLowerCase(Locale.ROOT).replace('_', ' ').replaceAll("\\s+", " ").trim();
    }

    @Override
    public String describe() {
        return "whether a stack is shown under a search and a kind";
    }
}
