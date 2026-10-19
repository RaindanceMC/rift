package de.raindancer.modules.rift.model;

import java.util.Locale;

/** The orders a rift sorts into. */
public enum SortMode {

    KIND("by kind"),
    NAME("A to Z"),
    AMOUNT("biggest stacks first");

    private final String label;

    SortMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public SortMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Read back from a file; anything unknown is the first. */
    public static SortMode parse(String written) {
        if (written != null) {
            for (SortMode mode : values()) {
                if (mode.name().equalsIgnoreCase(written.trim())) {
                    return mode;
                }
            }
        }
        return KIND;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
