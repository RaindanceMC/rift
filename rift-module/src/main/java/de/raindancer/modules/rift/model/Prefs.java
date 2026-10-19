package de.raindancer.modules.rift.model;

/**
 * How one player likes their rift.
 *
 * @param told how many fragments of the Rift's story they have been told of; 0 before their first look inside
 */
public record Prefs(SortMode sort, boolean autoSort, int told) {

    public static final Prefs DEFAULTS = new Prefs(SortMode.KIND, false, 0);

    public Prefs {
        sort = sort == null ? SortMode.KIND : sort;
        told = Math.max(0, told);
    }

    public Prefs withSort(SortMode mode) {
        return new Prefs(mode, autoSort, told);
    }

    public Prefs withAutoSort(boolean on) {
        return new Prefs(sort, on, told);
    }

    public Prefs withTold(int fragments) {
        return new Prefs(sort, autoSort, fragments);
    }
}
