package de.raindancer.modules.rift.rules;

import de.raindancer.modules.rift.model.PageTerms;
import de.raindancer.modules.rift.model.Pages;

import java.util.Collection;
import java.util.OptionalLong;

/**
 * How many pages a rift has: the starting ones, one free page per so many hours played actively (up to a most),
 * the bought ones and a bonus by permission — all together never past the most.
 */
public final class PageRule implements IRiftRule {

    public Pages pages(PageTerms terms, long activeMinutes, int bought, int bonus) {
        int most = Math.max(1, terms.most());
        int starting = Math.max(1, Math.min(most, terms.starting()));
        int mostFree = Math.max(0, terms.mostFree());
        long perPage = terms.freeEveryHours() <= 0 ? 0 : terms.freeEveryHours() * 60L;
        long played = Math.max(0, activeMinutes);
        int free = perPage == 0 ? 0 : (int) Math.min(mostFree, played / perPage);
        int boughtPages = Math.max(0, bought);
        int bonusPages = Math.max(0, bonus);

        long sum = (long) starting + free + boughtPages + bonusPages;
        int total = (int) Math.min(most, sum);
        boolean full = total >= most;
        OptionalLong next = perPage == 0 || free >= mostFree || full
                ? OptionalLong.empty()
                : OptionalLong.of((free + 1) * perPage - played);
        return new Pages(total, starting, free, boughtPages, bonusPages, next, full);
    }

    /** The bonus from {@code rains.rift.pages.<n>}: the highest one held, not the sum. */
    public int bonus(Collection<Integer> held) {
        return held.stream().mapToInt(Integer::intValue).filter(n -> n > 0).max().orElse(0);
    }

    @Override
    public String describe() {
        return "how many pages a rift has, and when the next free one comes";
    }
}
