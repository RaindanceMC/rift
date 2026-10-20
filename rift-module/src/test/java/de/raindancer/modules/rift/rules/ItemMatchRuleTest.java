package de.raindancer.modules.rift.rules;

import de.raindancer.core.content.items.ItemWords;
import de.raindancer.core.ui.choose.Category;
import de.raindancer.modules.rift.model.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** What a search and a filter let through. */
class ItemMatchRuleTest {

    private final ItemMatchRule rule = new ItemMatchRule();

    private final ItemWords sword = new ItemWords("Diamond Sword", "Excalibur",
            List.of("Sharpness V", "Mending"), Category.COMBAT);
    private final ItemWords logs = new ItemWords("Oak Log", null, List.of(), Category.BUILDING_BLOCKS);

    @Test
    @DisplayName("no search and no kind shows everything")
    void nothingChosen() {
        assertThat(rule.matches(sword, Filter.NONE)).isTrue();
        assertThat(Filter.NONE.isActive()).isFalse();
    }

    @Test
    @DisplayName("a search finds the item's name, its own name and its enchantments, whatever the case")
    void searching() {
        assertThat(rule.matches(sword, Filter.NONE.searching("diamond"))).isTrue();
        assertThat(rule.matches(sword, Filter.NONE.searching("EXCAL"))).isTrue();
        assertThat(rule.matches(sword, Filter.NONE.searching("mending"))).isTrue();
        assertThat(rule.matches(logs, Filter.NONE.searching("diamond"))).isFalse();
    }

    @Test
    @DisplayName("every word has to be there, in any order")
    void everyWord() {
        assertThat(rule.matches(sword, Filter.NONE.searching("sharp sword"))).isTrue();
        assertThat(rule.matches(sword, Filter.NONE.searching("sword oak"))).isFalse();
        assertThat(rule.matches(logs, Filter.NONE.searching("log   oak"))).isTrue();
    }

    @Test
    @DisplayName("underscores and the material's spelling find the same thing")
    void materialSpelling() {
        assertThat(rule.matches(logs, Filter.NONE.searching("oak_log"))).isTrue();
    }

    @Test
    @DisplayName("a kind lets only that kind through, and works together with a search")
    void kinds() {
        Filter combat = Filter.NONE.ofKind(Category.COMBAT);

        assertThat(rule.matches(sword, combat)).isTrue();
        assertThat(rule.matches(logs, combat)).isFalse();
        assertThat(rule.matches(sword, combat.searching("excalibur"))).isTrue();
        assertThat(rule.matches(sword, combat.searching("oak"))).isFalse();
    }

    @Test
    @DisplayName("a blank search is no search")
    void blank() {
        assertThat(Filter.NONE.searching("   ").isActive()).isFalse();
        assertThat(Filter.NONE.searching("  Oak ").query()).isEqualTo("oak");
    }

    @Test
    @DisplayName("kinds go round in the creative inventory's order, and back to all after the last")
    void cyclingKinds() {
        Filter filter = Filter.NONE;
        filter = filter.nextKind();
        assertThat(filter.kind()).isEqualTo(Category.values()[0]);
        for (int step = 1; step < Category.values().length; step++) {
            filter = filter.nextKind();
        }
        assertThat(filter.kind()).isEqualTo(Category.values()[Category.values().length - 1]);
        assertThat(filter.nextKind().kind()).isNull();
        assertThat(Filter.NONE.previousKind().kind()).isEqualTo(Category.values()[Category.values().length - 1]);
    }
}
