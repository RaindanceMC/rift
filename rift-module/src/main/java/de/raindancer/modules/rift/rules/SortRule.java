package de.raindancer.modules.rift.rules;

import de.raindancer.modules.rift.model.SortKey;
import de.raindancer.modules.rift.model.SortMode;

import java.util.Comparator;

/** The order each sort mode puts stacks in. */
public final class SortRule implements IRiftRule {

    private static final Comparator<SortKey> NAME = Comparator.comparing(SortKey::name, String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<SortKey> BIGGER_FIRST = Comparator.comparingInt(SortKey::amount).reversed();

    public Comparator<SortKey> order(SortMode mode) {
        return switch (mode) {
            case KIND -> Comparator.comparing((SortKey key) -> key.kind().ordinal()).thenComparing(NAME)
                    .thenComparing(BIGGER_FIRST);
            case NAME -> NAME.thenComparing(BIGGER_FIRST);
            case AMOUNT -> BIGGER_FIRST.thenComparing(NAME);
        };
    }

    @Override
    public String describe() {
        return "the order each sort mode puts a rift in";
    }
}
