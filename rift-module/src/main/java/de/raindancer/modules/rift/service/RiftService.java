package de.raindancer.modules.rift.service;

import de.raindancer.core.content.items.ItemWords;
import de.raindancer.core.data.stash.Stash;
import de.raindancer.core.social.economy.ItemSales;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.rift.model.Entry;
import de.raindancer.modules.rift.model.Filter;
import de.raindancer.modules.rift.model.Prefs;
import de.raindancer.modules.rift.model.SortMode;
import de.raindancer.modules.rift.rules.ItemMatchRule;
import de.raindancer.modules.rift.rules.SortRule;
import de.raindancer.modules.rift.store.RiftStore;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToIntFunction;

/**
 * Everybody's rift: putting things in, taking them out, looking for them, sorting and selling.
 *
 * <p>The window only ever shows copies. Every move is "take from one side, hand over exactly what the other side
 * accepted", and what does not fit goes back where it came from — never on the floor, never into nothing. A sale takes
 * the stacks out first and puts back every one the shop did not buy, even into a rift that filled up meanwhile.
 */
public final class RiftService implements IRiftService {

    /** Where work runs: the player's own thread for their inventory, another for the disk. */
    public interface Threads {

        Threads DIRECT = new Threads() {
            @Override
            public void onPlayer(Player player, Runnable task) {
                task.run();
            }

            @Override
            public void offThread(Runnable task) {
                task.run();
            }
        };

        void onPlayer(Player player, Runnable task);

        void offThread(Runnable task);
    }

    private static final int HOTBAR = 9;
    private static final int STORAGE_END = 36;

    private final RiftStore store;
    private final Messages messages;
    private final ToIntFunction<UUID> stacks;
    private final Threads threads;
    private final ItemMatchRule matching = new ItemMatchRule();
    private final SortRule sorting = new SortRule();
    private final Map<UUID, Stash> open = new ConcurrentHashMap<>();
    private final Set<UUID> selling = ConcurrentHashMap.newKeySet();

    /** @param stacks how many stacks somebody's pages hold now */
    public RiftService(RiftStore store, Messages messages, ToIntFunction<UUID> stacks, Threads threads) {
        this.store = store;
        this.messages = messages;
        this.stacks = stacks;
        this.threads = threads;
    }

    public Stash of(UUID owner) {
        return open.computeIfAbsent(owner, id -> store.load(id, stacks.applyAsInt(id)));
    }

    /** The rift if it is in memory right now, without reading it from disk: for what only looks. */
    public java.util.Optional<Stash> held(UUID owner) {
        return java.util.Optional.ofNullable(open.get(owner));
    }

    /** Pages came or went: the rift holds that many from now on, and never drops what is in. */
    public void resize(UUID owner) {
        Stash rift = open.get(owner);
        if (rift != null) {
            rift.capacity(stacks.applyAsInt(owner));
        }
    }

    /** Every rift in memory, after the settings changed. */
    public void resizeAll() {
        open.keySet().forEach(this::resize);
    }

    public Prefs prefs(UUID owner) {
        return store.prefs(owner);
    }

    public void prefs(UUID owner, Prefs changed) {
        store.prefs(owner, changed);
    }

    /** What a screen shows under this filter, each with its real place in the rift. */
    public List<Entry> view(UUID owner, Filter filter) {
        List<ItemStack> items = of(owner).items();
        List<Entry> shown = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            ItemStack item = items.get(index);
            if (!filter.isActive() || matching.matches(ItemWords.of(item), filter)) {
                shown.add(new Entry(index, item));
            }
        }
        return shown;
    }

    /** @return how many of this went in; the caller takes exactly that many away */
    public int deposit(Player owner, ItemStack item) {
        Stash rift = of(owner.getUniqueId());
        int accepted = rift.deposit(item);
        if (accepted > 0) {
            keepSorted(owner.getUniqueId(), rift);
            save(owner.getUniqueId());
        }
        return accepted;
    }

    /** Takes one entry into the owner's inventory; what does not fit stays in the rift. */
    public void take(Player owner, Entry entry) {
        Stash rift = of(owner.getUniqueId());
        ItemStack taken = rift.take(entry.index(), entry.shown());
        if (taken == null) {
            return;
        }
        if (!hand(owner, rift, taken)) {
            messages.send(owner, "rift.inventory-full");
        }
        save(owner.getUniqueId());
    }

    /** Takes all of these that fit. */
    public void takeAll(Player owner, List<Entry> entries) {
        Stash rift = of(owner.getUniqueId());
        int moved = 0;
        boolean full = false;
        for (Entry entry : backwards(entries)) {
            ItemStack taken = rift.take(entry.index(), entry.shown());
            if (taken == null) {
                continue;
            }
            if (hand(owner, rift, taken)) {
                moved++;
            } else {
                full = true;
            }
        }
        if (moved > 0 || full) {
            save(owner.getUniqueId());
        }
        messages.send(owner, full ? "rift.took-some" : "rift.took-all", "count", String.valueOf(moved));
    }

    /** The inventory's storage rows into the rift, and the hotbar with {@code hotbarToo}. Armour stays on. */
    public void depositAll(Player owner, boolean hotbarToo) {
        Stash rift = of(owner.getUniqueId());
        PlayerInventory inventory = owner.getInventory();
        int stored = 0;
        boolean full = false;
        for (int slot = hotbarToo ? 0 : HOTBAR; slot < STORAGE_END; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            int accepted = rift.deposit(stack);
            if (accepted < stack.getAmount()) {
                full = true;
            }
            if (accepted <= 0) {
                continue;
            }
            ItemStack left = stack.clone();
            left.setAmount(stack.getAmount() - accepted);
            inventory.setItem(slot, left.getAmount() <= 0 ? null : left);
            stored++;
        }
        if (stored > 0) {
            keepSorted(owner.getUniqueId(), rift);
            save(owner.getUniqueId());
        }
        messages.send(owner, full ? "rift.stored-some" : "rift.stored-all", "count", String.valueOf(stored));
    }

    /** @return whether anything moved */
    public boolean sort(Player owner, SortMode mode) {
        boolean moved = of(owner.getUniqueId()).sort(order(mode));
        if (moved) {
            save(owner.getUniqueId());
            messages.send(owner, "rift.sort.done", "order", mode.label());
        } else {
            messages.send(owner, "rift.sort.already", "order", mode.label());
        }
        return moved;
    }

    public boolean selling(UUID owner) {
        return selling.contains(owner);
    }

    /** Whether anything among these is something the shop here buys — for greying the button out. */
    public boolean anythingSellable(List<Entry> entries) {
        return ItemSales.isOpen() && entries.stream().anyMatch(entry -> ItemSales.buys(entry.shown()));
    }

    /**
     * Sells what the shop buys among these: takes them out, lets the shop pay, puts back whatever it did not buy.
     * One sale per player at a time.
     *
     * @param done run on the owner's thread once the sale is settled, to draw the screen again
     */
    public void sell(Player owner, List<Entry> entries, Runnable done) {
        UUID id = owner.getUniqueId();
        if (!ItemSales.isOpen()) {
            messages.send(owner, "rift.sell.no-shop");
            return;
        }
        if (!selling.add(id)) {
            messages.send(owner, "rift.sell.busy");
            return;
        }
        Stash rift = of(id);
        List<ItemStack> taken = new ArrayList<>();
        for (Entry entry : backwards(entries)) {
            if (!ItemSales.buys(entry.shown())) {
                continue;
            }
            ItemStack stack = rift.take(entry.index(), entry.shown());
            if (stack != null) {
                taken.add(stack);
            }
        }
        if (taken.isEmpty()) {
            selling.remove(id);
            messages.send(owner, "rift.sell.nothing");
            return;
        }
        ItemSales.sell(owner, taken).handle((sold, failure) -> {
            // Whatever went wrong, a stack is only gone when the shop said it bought it.
            boolean answered = failure == null && sold != null && sold.size() == taken.size();
            try {
                for (int index = 0; index < taken.size(); index++) {
                    if (!answered || !Boolean.TRUE.equals(sold.get(index))) {
                        putBack(rift, taken.get(index));
                    }
                }
                keepSorted(id, rift);
                save(id);
            } finally {
                selling.remove(id);
            }
            if (owner.isOnline()) {
                threads.onPlayer(owner, done);
            }
            return null;
        });
    }

    /** Writes every rift now, on this thread — for the shutdown, which has no later. */
    public boolean flushNow() {
        boolean all = true;
        for (Map.Entry<UUID, Stash> rift : open.entrySet()) {
            all &= store.save(rift.getKey(), rift.getValue().contents());
        }
        return all;
    }

    /** Somebody left: written, and let go of unless a sale still needs it. */
    public void forget(UUID owner) {
        Stash rift = open.get(owner);
        if (rift == null) {
            return;
        }
        Stash.Contents contents = rift.contents();
        threads.offThread(() -> store.save(owner, contents));
        if (!selling.contains(owner)) {
            open.remove(owner, rift);
        }
    }

    private void keepSorted(UUID owner, Stash rift) {
        Prefs prefs = store.prefs(owner);
        if (prefs.autoSort()) {
            rift.sort(order(prefs.sort()));
        }
    }

    private Comparator<ItemStack> order(SortMode mode) {
        Comparator<de.raindancer.modules.rift.model.SortKey> keys = sorting.order(mode);
        return (one, other) -> keys.compare(sortKey(one), sortKey(other));
    }

    private void save(UUID owner) {
        Stash rift = open.get(owner);
        if (rift != null) {
            Stash.Contents contents = rift.contents();
            threads.offThread(() -> store.save(owner, contents));
        }
    }

    /** Highest index first, so taking one does not move the next. */
    private static List<Entry> backwards(List<Entry> entries) {
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt(Entry::index).reversed());
        return sorted;
    }

    /**
     * Back into the rift, even when it filled up while the stack was out: the limit is lifted for exactly this one
     * stack, so it is over capacity rather than gone. Deposits are refused until it is under again.
     */
    private static void putBack(Stash rift, ItemStack stack) {
        int accepted = rift.deposit(stack);
        if (accepted >= stack.getAmount()) {
            return;
        }
        ItemStack rest = stack.asQuantity(stack.getAmount() - accepted);
        synchronized (rift) {
            int limit = rift.capacity();
            rift.capacity(rift.size() + 1);
            rift.deposit(rest);
            rift.capacity(limit);
        }
    }

    /**
     * Into the owner's inventory; whatever does not fit goes back into the rift.
     *
     * @return whether all of it fitted
     */
    private static boolean hand(Player owner, Stash rift, ItemStack item) {
        Map<Integer, ItemStack> leftOver = owner.getInventory().addItem(item);
        leftOver.values().forEach(spare -> putBack(rift, spare));
        return leftOver.isEmpty();
    }

    @Override
    public String describe() {
        return "everybody's rift: in, out, search, sort and sell";
    }

    /** Sorted by what a player reads: the given name when there is one, otherwise the material's. */
    private static de.raindancer.modules.rift.model.SortKey sortKey(org.bukkit.inventory.ItemStack item) {
        ItemWords words = ItemWords.of(item);
        return new de.raindancer.modules.rift.model.SortKey(words.kind(), words.shown(), item.getAmount());
    }
}
