package de.raindancer.modules.rift.model;

import de.raindancer.core.ui.choose.Category;

import java.util.Locale;

/**
 * What the rift is showing: a search, a kind, both or neither.
 *
 * @param query lower case, trimmed; empty for no search
 * @param kind  null for every kind
 */
public record Filter(String query, Category kind) {

    public static final Filter NONE = new Filter("", null);

    public Filter {
        query = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isActive() {
        return !query.isEmpty() || kind != null;
    }

    public boolean isSearching() {
        return !query.isEmpty();
    }

    public Filter searching(String text) {
        return new Filter(text, kind);
    }

    public Filter ofKind(Category category) {
        return new Filter(query, category);
    }

    /** All, then the creative inventory's tabs in order, then all again. */
    public Filter nextKind() {
        Category[] kinds = Category.values();
        if (kind == null) {
            return ofKind(kinds[0]);
        }
        return ofKind(kind.ordinal() + 1 < kinds.length ? kinds[kind.ordinal() + 1] : null);
    }

    public Filter previousKind() {
        Category[] kinds = Category.values();
        if (kind == null) {
            return ofKind(kinds[kinds.length - 1]);
        }
        return ofKind(kind.ordinal() > 0 ? kinds[kind.ordinal() - 1] : null);
    }
}
