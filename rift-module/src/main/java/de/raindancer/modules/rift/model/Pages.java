package de.raindancer.modules.rift.model;

import java.util.OptionalLong;

/**
 * How big somebody's rift is right now, and where each page came from.
 *
 * @param minutesToNextFree active minutes until playtime earns the next page; empty when none is coming
 * @param full              no more pages can be had at all
 */
public record Pages(int total, int starting, int free, int bought, int bonus, OptionalLong minutesToNextFree,
                    boolean full) {

    /** The four rows above the toolbar. */
    public static final int STACKS_PER_PAGE = 36;

    public int stacks() {
        return total * STACKS_PER_PAGE;
    }
}
