package de.raindancer.modules.rift.model;

/** How one player likes their rift. */
public record Prefs(SortMode sort, boolean autoSort) {

    public static final Prefs DEFAULTS = new Prefs(SortMode.KIND, false);

    public Prefs {
        sort = sort == null ? SortMode.KIND : sort;
    }

    public Prefs withSort(SortMode mode) {
        return new Prefs(mode, autoSort);
    }

    public Prefs withAutoSort(boolean on) {
        return new Prefs(sort, on);
    }
}
