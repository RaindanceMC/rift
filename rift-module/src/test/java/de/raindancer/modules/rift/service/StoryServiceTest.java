package de.raindancer.modules.rift.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.rift.rules.StoryRule;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("The story as a book")
class StoryServiceTest {

    @TempDir
    Path folder;

    private Messages messages;

    @BeforeEach
    void wording() throws Exception {
        messages = new Messages(folder.resolve("messages.yml"));
        messages.load(Files.newInputStream(Path.of("src/main/resources/de/raindancer/modules/rift/messages.yml")));
    }

    private static String plain(Component page) {
        return PlainTextComponentSerializer.plainText().serialize(page);
    }

    @Test
    @DisplayName("a cover, then all twelve fragments: the unlocked ones told, the rest torn out")
    void book() {
        Book book = StoryService.book(messages, 3);
        assertThat(book.pages()).hasSize(1 + StoryRule.FRAGMENTS);
        assertThat(plain(book.pages().get(0))).contains("3").contains("12");
        assertThat(plain(book.pages().get(1))).contains("The Crack");
        assertThat(plain(book.pages().get(3))).contains("Why it grows");
        assertThat(plain(book.pages().get(4))).contains("torn out").doesNotContain("Tollkeeper");
        assertThat(plain(book.pages().get(12))).contains("torn out");
    }

    @Test
    @DisplayName("a full rift reads to the end")
    void whole() {
        Book book = StoryService.book(messages, 12);
        assertThat(book.pages()).noneMatch(page -> plain(page).contains("torn out"));
        assertThat(plain(book.pages().get(12))).contains("The Seal");
        assertThat(plain(book.title())).isNotBlank();
    }

    @Test
    @DisplayName("every fragment has a title and a text no longer than a book page holds")
    void fragments() {
        for (int fragment = 1; fragment <= StoryRule.FRAGMENTS; fragment++) {
            assertThat(messages.has("rift.story.fragment-" + fragment + ".title")).as("title " + fragment).isTrue();
            String text = messages.raw("rift.story.fragment-" + fragment + ".text");
            assertThat(text).as("text " + fragment).isNotBlank();
            assertThat(plain(messages.get("rift.story.fragment-" + fragment + ".text")).length())
                    .as("fragment " + fragment + " overflows its page").isLessThanOrEqualTo(190);
        }
    }
}
