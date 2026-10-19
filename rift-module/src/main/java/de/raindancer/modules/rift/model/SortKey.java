package de.raindancer.modules.rift.model;

import de.raindancer.core.ui.choose.Category;

/** What sorting looks at in one stack. */
public record SortKey(Category kind, String name, int amount) {
}
