package de.raindancer.modules.rift.rules;

import de.raindancer.core.world.time.Times;
import de.raindancer.modules.rift.model.Pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/** What the rift screen says about somebody's pages. */
public final class ScreenRule implements IRiftRule {

    /** @param used how many stacks are in the rift; empty while it has not been read yet */
    public String you(Pages pages, OptionalInt used) {
        StringBuilder out = new StringBuilder("<gold><bold>Your rift</bold></gold>");
        List<String> parts = new ArrayList<>();
        parts.add(pages.starting() + " start");
        if (pages.free() > 0) {
            parts.add(pages.free() + " played");
        }
        if (pages.bought() > 0) {
            parts.add(pages.bought() + " bought");
        }
        if (pages.bonus() > 0) {
            parts.add(pages.bonus() + " bonus");
        }
        out.append("\n<gray>Pages <white>").append(pages.total()).append(" <dark_gray>(")
                .append(String.join(", ", parts)).append(")");
        if (used.isPresent()) {
            int stacks = pages.stacks();
            out.append("\n<gray>Filled <white>").append(used.getAsInt()).append("</white><dark_gray>/<white>")
                    .append(stacks).append(" <dark_gray>(").append(Math.min(100, used.getAsInt() * 100 / stacks))
                    .append("%)");
        }
        if (pages.full()) {
            out.append("\n<gray>The most pages a rift can have");
        } else if (pages.minutesToNextFree().isPresent()) {
            out.append("\n<gray>Next free page in <white>")
                    .append(Times.describe(Duration.ofMinutes(pages.minutesToNextFree().getAsLong())));
        }
        return out.toString();
    }

    @Override
    public String describe() {
        return "what the rift screen says about somebody's pages";
    }
}
