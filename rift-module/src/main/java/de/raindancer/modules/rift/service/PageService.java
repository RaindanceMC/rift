package de.raindancer.modules.rift.service;

import de.raindancer.core.social.economy.BuyableSlots;
import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.rift.RiftSettings;
import de.raindancer.modules.rift.model.Pages;
import de.raindancer.modules.rift.rules.PageRule;
import de.raindancer.modules.rift.util.PermissionNodes;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/**
 * How many pages somebody's rift has, and buying the next one.
 *
 * <p>The bonus by permission is read whenever the player is looked at online and remembered for when they are not, so
 * an offline rift is never shrunk below what it held a minute ago.
 */
public final class PageService implements IRiftService {

    private final Supplier<RiftSettings> settings;
    private final ToLongFunction<UUID> activeMinutes;
    private final BuyableSlots bought;
    private final PageRule rule = new PageRule();
    private final Map<UUID, Integer> bonus = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> seen = new ConcurrentHashMap<>();

    /** @param activeMinutes Core's playtime, away time not counted */
    public PageService(Supplier<RiftSettings> settings, ToLongFunction<UUID> activeMinutes, BuyableSlots bought) {
        this.settings = settings;
        this.activeMinutes = activeMinutes;
        this.bought = bought;
    }

    public Pages pages(UUID player) {
        return rule.pages(settings.get().pageTerms(), activeMinutes.applyAsLong(player), bought.bought(player),
                bonus.getOrDefault(player, 0));
    }

    /** Reads their bonus nodes again; the pages as they are now. */
    public Pages pages(Player player) {
        List<Integer> held = new ArrayList<>();
        int most = settings.get().mostPagesClamped();
        for (int pages = 1; pages <= most; pages++) {
            if (player.hasPermission(PermissionNodes.bonusPages(pages))) {
                held.add(pages);
            }
        }
        bonus.put(player.getUniqueId(), rule.bonus(held));
        return pages(player.getUniqueId());
    }

    /** What the next page costs this player now, or empty when one cannot be bought. */
    public Optional<Money> nextPrice(UUID player) {
        RiftSettings live = settings.get();
        if (pages(player).full() || bought.why(player, live.pageBuying()).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(bought.quote(player, live.pageBuying()));
    }

    /** Why the next page cannot be bought, or empty when it can. */
    public Optional<BuyableSlots.Outcome> whyNot(UUID player) {
        if (pages(player).full()) {
            return Optional.of(BuyableSlots.Outcome.MAXED);
        }
        return bought.why(player, settings.get().pageBuying());
    }

    /** Buys one page; the charge and the file off the server thread. */
    public CompletableFuture<BuyableSlots.Purchase> buy(UUID player, Executor offThread) {
        if (pages(player).full()) {
            return CompletableFuture.completedFuture(
                    new BuyableSlots.Purchase(BuyableSlots.Outcome.MAXED, Money.ZERO, bought.bought(player)));
        }
        return bought.buyOffThread(player, settings.get().pageBuying(), offThread);
    }

    /**
     * Looks at an online player again — at join, and every minute — and says whether their rift grew since the last
     * look, so they can be told when playtime earned a page.
     *
     * @return the new number of pages, when it went up
     */
    public Optional<Integer> noticed(Player player) {
        int now = pages(player).total();
        Integer before = seen.put(player.getUniqueId(), now);
        return before != null && now > before ? Optional.of(now) : Optional.empty();
    }

    @Override
    public String describe() {
        return "how many pages a rift has, and buying the next";
    }
}
