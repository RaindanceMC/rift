package de.raindancer.modules.rift.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("The story unfolds with the rift: one fragment per page")
class StoryRuleTest {

    private final StoryRule rule = new StoryRule();

    @Test
    @DisplayName("as many fragments as pages, at least the first, at most all twelve")
    void unlocked() {
        assertThat(rule.unlocked(0)).isEqualTo(1);
        assertThat(rule.unlocked(1)).isEqualTo(1);
        assertThat(rule.unlocked(5)).isEqualTo(5);
        assertThat(rule.unlocked(40)).isEqualTo(StoryRule.FRAGMENTS);
    }

    @Test
    @DisplayName("each fragment is announced once: the ones past what was told, up to what is unlocked")
    void announce() {
        assertThat(rule.toAnnounce(1, 3)).containsExactly(2, 3);
        assertThat(rule.toAnnounce(3, 3)).isEmpty();
        assertThat(rule.toAnnounce(5, 3)).as("a rift that shrank tells nothing twice").isEmpty();
        assertThat(rule.toAnnounce(11, 99)).containsExactly(12);
    }
}
