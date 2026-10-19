package de.raindancer.modules.rift.rules;

import de.raindancer.core.ui.choose.Category;
import de.raindancer.modules.rift.model.SortKey;
import de.raindancer.modules.rift.model.SortMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The orders a rift can be sorted into. */
class SortRuleTest {

    private final SortRule rule = new SortRule();

    private final SortKey stone = new SortKey(Category.BUILDING_BLOCKS, "Stone", 12);
    private final SortKey moreStone = new SortKey(Category.BUILDING_BLOCKS, "Stone", 64);
    private final SortKey apple = new SortKey(Category.FOOD, "Apple", 3);
    private final SortKey sword = new SortKey(Category.COMBAT, "Diamond Sword", 1);

    private List<SortKey> sorted(SortMode mode) {
        List<SortKey> keys = new ArrayList<>(List.of(sword, stone, apple, moreStone));
        keys.sort(rule.order(mode));
        return keys;
    }

    @Test
    @DisplayName("by kind: the creative inventory's tabs, then the name, the bigger stack first")
    void byKind() {
        assertThat(sorted(SortMode.KIND)).containsExactly(moreStone, stone, apple, sword);
    }

    @Test
    @DisplayName("by name: A to Z, the bigger stack first")
    void byName() {
        assertThat(sorted(SortMode.NAME)).containsExactly(apple, sword, moreStone, stone);
    }

    @Test
    @DisplayName("by amount: the biggest stacks first, ties by name")
    void byAmount() {
        assertThat(sorted(SortMode.AMOUNT)).containsExactly(moreStone, stone, apple, sword);
    }

    @Test
    @DisplayName("the modes go round")
    void cycling() {
        assertThat(SortMode.KIND.next()).isEqualTo(SortMode.NAME);
        assertThat(SortMode.values()[SortMode.values().length - 1].next()).isEqualTo(SortMode.KIND);
        assertThat(SortMode.parse("amount")).isEqualTo(SortMode.AMOUNT);
        assertThat(SortMode.parse("nonsense")).isEqualTo(SortMode.KIND);
        assertThat(SortMode.parse(null)).isEqualTo(SortMode.KIND);
    }
}
