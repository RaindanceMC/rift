package de.raindancer.modules.rift.rules;

import java.util.ArrayList;
import java.util.List;

/** How much of the Rift's story a player has: one fragment per page of their rift, so the tale grows with it. */
public final class StoryRule implements IRiftRule {

    /** As many as a rift's pages can ever be. */
    public static final int FRAGMENTS = 12;

    @Override
    public String describe() {
        return "one fragment of the story per page of the rift";
    }

    public int unlocked(int pages) {
        return Math.max(1, Math.min(FRAGMENTS, pages));
    }

    /** The fragments to announce now: past those already told, up to those unlocked. */
    public List<Integer> toAnnounce(int told, int pages) {
        List<Integer> fresh = new ArrayList<>();
        for (int fragment = told + 1; fragment <= unlocked(pages); fragment++) {
            fresh.add(fragment);
        }
        return fresh;
    }
}
