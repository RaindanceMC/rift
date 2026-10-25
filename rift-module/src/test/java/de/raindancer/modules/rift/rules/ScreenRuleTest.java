package de.raindancer.modules.rift.rules;

import de.raindancer.modules.rift.model.Pages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("What the rift screen says")
class ScreenRuleTest {

    private final ScreenRule rule = new ScreenRule();

    @Test
    @DisplayName("pages, where they came from, how full the rift is and when the next free page comes")
    void growing() {
        Pages pages = new Pages(4, 1, 2, 1, 0, OptionalLong.of(135), false);
        String text = rule.you(pages, OptionalInt.of(100));
        assertThat(text).startsWith("<gold><bold>Your rift</bold></gold>")
                .contains("<gray>Pages <white>4 <dark_gray>(1 start, 2 played, 1 bought)")
                .contains("<gray>Filled <white>100</white><dark_gray>/<white>144 <dark_gray>(69%)")
                .contains("<gray>Next free page in <white>2 hours, 15 minutes");
    }

    @Test
    @DisplayName("a bonus by permission is named, and the parts that are zero are left out")
    void bonus() {
        Pages pages = new Pages(3, 1, 0, 0, 3, OptionalLong.empty(), false);
        assertThat(rule.you(pages, OptionalInt.of(0))).contains("(1 start, 3 bonus)").contains("0</white><dark_gray>/<white>108 <dark_gray>(0%)");
    }

    @Test
    @DisplayName("a rift not read yet shows no fill, a full one says no page can be had")
    void unreadAndFull() {
        Pages full = new Pages(12, 1, 3, 8, 0, OptionalLong.empty(), true);
        String text = rule.you(full, OptionalInt.empty());
        assertThat(text).doesNotContain("Filled").contains("<gray>Pages <white>12").contains("The most pages")
                .doesNotContain("Next free page");
    }

    @Test
    @DisplayName("a rift over-full after shrinking reads at most 100 percent")
    void overFull() {
        Pages pages = new Pages(1, 1, 0, 0, 0, OptionalLong.empty(), false);
        assertThat(rule.you(pages, OptionalInt.of(40))).contains("(100%)");
    }
}
