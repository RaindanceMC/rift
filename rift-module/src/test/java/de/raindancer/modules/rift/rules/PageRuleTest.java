package de.raindancer.modules.rift.rules;

import de.raindancer.modules.rift.model.PageTerms;
import de.raindancer.modules.rift.model.Pages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** How many pages somebody's rift has, and what the next one takes. */
class PageRuleTest {

    private final PageRule rule = new PageRule();
    /** One to start, a free page every 8 hours played (three at most), twelve in all. */
    private final PageTerms terms = new PageTerms(1, 12, 8, 3);

    private static long hours(long hours) {
        return hours * 60;
    }

    @Test
    @DisplayName("a newcomer has the starting page and is told when the next free one comes")
    void newcomer() {
        Pages pages = rule.pages(terms, hours(0) + 30, 0, 0);

        assertThat(pages.total()).isEqualTo(1);
        assertThat(pages.minutesToNextFree()).hasValue(hours(8) - 30);
        assertThat(pages.full()).isFalse();
    }

    @Test
    @DisplayName("playtime earns free pages up to the most, then stops counting down")
    void playtime() {
        assertThat(rule.pages(terms, hours(16), 0, 0).free()).isEqualTo(2);
        Pages veteran = rule.pages(terms, hours(500), 0, 0);

        assertThat(veteran.free()).isEqualTo(3);
        assertThat(veteran.total()).isEqualTo(4);
        assertThat(veteran.minutesToNextFree()).isEmpty();
    }

    @Test
    @DisplayName("bought and bonus pages add up, never past the most")
    void addingUp() {
        Pages pages = rule.pages(terms, hours(8), 2, 3);
        assertThat(pages.total()).isEqualTo(1 + 1 + 2 + 3);

        Pages capped = rule.pages(terms, hours(500), 20, 20);
        assertThat(capped.total()).isEqualTo(12);
        assertThat(capped.full()).isTrue();
        assertThat(capped.minutesToNextFree()).as("no free page is coming when there is no room").isEmpty();
    }

    @Test
    @DisplayName("free pages switched off are never earned and never promised")
    void noFreePages() {
        Pages pages = rule.pages(new PageTerms(2, 12, 0, 3), hours(1000), 0, 0);

        assertThat(pages.total()).isEqualTo(2);
        assertThat(pages.minutesToNextFree()).isEmpty();
    }

    @Test
    @DisplayName("nonsense terms are clamped: at least one page, the start never above the most")
    void clamped() {
        Pages pages = rule.pages(new PageTerms(0, 0, -5, -1), -100, -3, -2);
        assertThat(pages.total()).isEqualTo(1);

        assertThat(rule.pages(new PageTerms(20, 5, 8, 3), 0, 0, 0).total()).isEqualTo(5);
    }

    @Test
    @DisplayName("a page holds 36 stacks, and the capacity follows the pages")
    void capacity() {
        assertThat(rule.pages(terms, hours(16), 1, 0).stacks()).isEqualTo(4 * Pages.STACKS_PER_PAGE);
        assertThat(Pages.STACKS_PER_PAGE).isEqualTo(36);
    }

    @Test
    @DisplayName("the bonus from permission nodes is the highest one held")
    void bonusFromNodes() {
        assertThat(rule.bonus(java.util.List.of(2, 5, 3))).isEqualTo(5);
        assertThat(rule.bonus(java.util.List.of())).isZero();
    }
}
