package de.raindancer.modules.rift.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.rift.model.Prefs;
import de.raindancer.modules.rift.rules.StoryRule;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The Rift's story, "Notes from the Fold": twelve fragments, one per page a rift can have, so it is read as the rift
 * grows. The words are in messages.yml, rewritable like every other line; this only decides which are shown.
 */
public final class StoryService implements IRiftService {

    private final RiftService rift;
    private final PageService pages;
    private final Messages messages;
    private final StoryRule rule = new StoryRule();

    public StoryService(RiftService rift, PageService pages, Messages messages) {
        this.rift = rift;
        this.pages = pages;
        this.messages = messages;
    }

    @Override
    public String describe() {
        return "the Rift's story, a fragment per page";
    }

    /** The book for a rift of so many pages: a cover, then every fragment — told, or torn out. */
    public static Book book(Messages messages, int riftPages) {
        StoryRule rule = new StoryRule();
        int open = rule.unlocked(riftPages);
        List<Component> sheets = new ArrayList<>();
        sheets.add(messages.get("rift.story.cover", "unlocked", String.valueOf(open),
                "all", String.valueOf(StoryRule.FRAGMENTS)));
        for (int fragment = 1; fragment <= StoryRule.FRAGMENTS; fragment++) {
            String key = "rift.story.fragment-" + fragment;
            if (fragment <= open) {
                sheets.add(messages.get("rift.story.page-head", "number", String.valueOf(fragment))
                        .appendNewline().append(messages.get(key + ".title"))
                        .appendNewline().appendNewline().append(messages.get(key + ".text")));
            } else {
                sheets.add(messages.get("rift.story.locked", "number", String.valueOf(fragment)));
            }
        }
        return Book.book(messages.get("rift.story.title"), messages.get("rift.story.author"), sheets);
    }

    /** Opens the story so far. On the player's own thread. */
    public void open(Player reader) {
        reader.openBook(book(messages, pages.pages(reader).total()));
    }

    /** The first look into one's rift: a hint that there is a story, and the fragments it already holds counted as told. */
    public void firstLook(Player player) {
        Prefs prefs = rift.prefs(player.getUniqueId());
        if (prefs.told() > 0) {
            return;
        }
        messages.send(player, "rift.story.intro");
        rift.prefs(player.getUniqueId(), prefs.withTold(rule.unlocked(pages.pages(player).total())));
    }

    /** The rift grew: each fragment it now holds that was not told yet is announced, once. */
    public void grew(Player player, int riftPages) {
        Prefs prefs = rift.prefs(player.getUniqueId());
        if (prefs.told() == 0) {
            // Never looked inside: the first look counts them all at once.
            return;
        }
        List<Integer> fresh = rule.toAnnounce(prefs.told(), riftPages);
        if (fresh.isEmpty()) {
            return;
        }
        for (int fragment : fresh) {
            messages.send(player, "rift.story.unlocked", "number", String.valueOf(fragment),
                    "title", PlainTextComponentSerializer.plainText().serialize(
                            messages.get("rift.story.fragment-" + fragment + ".title")));
        }
        rift.prefs(player.getUniqueId(), prefs.withTold(fresh.get(fresh.size() - 1)));
    }
}
