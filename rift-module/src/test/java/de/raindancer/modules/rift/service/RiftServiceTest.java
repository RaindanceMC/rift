package de.raindancer.modules.rift.service;

import de.raindancer.core.data.nbt.ItemText;
import de.raindancer.core.social.economy.ItemBuyer;
import de.raindancer.core.social.economy.ItemSales;
import de.raindancer.core.testkit.TestItems;
import de.raindancer.core.ui.choose.Category;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.rift.PlainBytes;
import de.raindancer.modules.rift.model.Entry;
import de.raindancer.modules.rift.model.Filter;
import de.raindancer.modules.rift.model.SortMode;
import de.raindancer.modules.rift.store.RiftStore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Putting things in, taking them out, looking for them, sorting them and selling them. */
class RiftServiceTest {

    @TempDir
    Path folder;

    private final Messages messages = mock(Messages.class);
    private final Player lilly = mock(Player.class);
    private final PlayerInventory inventory = mock(PlayerInventory.class);
    private final UUID id = UUID.randomUUID();
    private final List<Runnable> saves = new ArrayList<>();
    private final AtomicInteger stacks = new AtomicInteger(36);
    private RiftStore store;
    private RiftService rift;

    @BeforeEach
    void setUp() {
        when(lilly.getUniqueId()).thenReturn(id);
        when(lilly.getInventory()).thenReturn(inventory);
        when(lilly.isOnline()).thenReturn(true);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
        store = new RiftStore(folder, new ItemText(new PlainBytes()));
        rift = new RiftService(store, messages, who -> stacks.get(), new RiftService.Threads() {
            @Override
            public void onPlayer(Player player, Runnable task) {
                task.run();
            }

            @Override
            public void offThread(Runnable task) {
                saves.add(task);
            }
        });
    }

    @AfterEach
    void tearDown() {
        ItemSales.clear();
    }

    private static ItemStack of(Material material, int amount) {
        return TestItems.of(material, amount);
    }

    private List<String> contents() {
        return rift.of(id).items().stream().map(item -> item.getType() + "x" + item.getAmount()).toList();
    }

    @Test
    @DisplayName("what goes in is written to disk, and comes back after a restart")
    void survivesARestart() {
        assertThat(rift.deposit(lilly, of(Material.DIRT, 10))).isEqualTo(10);
        assertThat(saves).isNotEmpty();
        saves.forEach(Runnable::run);

        RiftService again = new RiftService(new RiftStore(folder, new ItemText(new PlainBytes())), messages,
                who -> 36, RiftService.Threads.DIRECT);

        assertThat(again.of(id).items()).extracting(ItemStack::getType).containsExactly(Material.DIRT);
    }

    @Test
    @DisplayName("armour is just another item in a rift: no stands")
    void noStands() {
        rift.deposit(lilly, of(Material.DIAMOND_HELMET, 1));

        assertThat(rift.of(id).hasArmour()).isFalse();
        assertThat(contents()).containsExactly("DIAMOND_HELMETx1");
    }

    @Test
    @DisplayName("a filtered view keeps every entry's real place, so taking from it takes the right one")
    void filteredView() {
        rift.deposit(lilly, of(Material.DIRT, 10));
        rift.deposit(lilly, of(Material.APPLE, 3));
        rift.deposit(lilly, of(Material.STONE, 5));

        List<Entry> food = rift.view(id, Filter.NONE.ofKind(Category.FOOD));
        assertThat(food).extracting(Entry::index).containsExactly(1);

        List<Entry> searched = rift.view(id, Filter.NONE.searching("ston"));
        assertThat(searched).extracting(Entry::index).containsExactly(2);

        rift.take(lilly, searched.getFirst());
        assertThat(contents()).containsExactly("DIRTx10", "APPLEx3");
        verify(inventory).addItem(any(ItemStack[].class));
    }

    @Test
    @DisplayName("an entry taken from a stale screen is not taken twice")
    void staleScreen() {
        rift.deposit(lilly, of(Material.DIRT, 10));
        Entry shown = rift.view(id, Filter.NONE).getFirst();

        rift.take(lilly, shown);
        rift.take(lilly, shown);

        assertThat(contents()).isEmpty();
        verify(inventory, org.mockito.Mockito.times(1)).addItem(any(ItemStack[].class));
    }

    @Test
    @DisplayName("what does not fit into the inventory stays in the rift")
    void inventoryFull() {
        rift.deposit(lilly, of(Material.DIRT, 10));
        HashMap<Integer, ItemStack> left = new HashMap<>();
        left.put(0, of(Material.DIRT, 4));
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(left);

        rift.take(lilly, rift.view(id, Filter.NONE).getFirst());

        assertThat(contents()).containsExactly("DIRTx4");
        verify(messages).send(lilly, "rift.inventory-full");
    }

    @Test
    @DisplayName("a full rift says so, and a new page makes room at once")
    void growing() {
        stacks.set(1);
        rift.deposit(lilly, of(Material.DIRT, 64));
        assertThat(rift.deposit(lilly, of(Material.STONE, 1))).isZero();

        stacks.set(2);
        rift.resize(id);

        assertThat(rift.deposit(lilly, of(Material.STONE, 1))).isEqualTo(1);
    }

    @Test
    @DisplayName("sorting puts things in order and merges half stacks; sorting again is nothing to do")
    void sorting() {
        rift.deposit(lilly, of(Material.STONE, 5));
        rift.deposit(lilly, of(Material.APPLE, 3));
        rift.deposit(lilly, of(Material.DIRT, 10));

        assertThat(rift.sort(lilly, SortMode.NAME)).isTrue();
        assertThat(contents()).containsExactly("APPLEx3", "DIRTx10", "STONEx5");
        assertThat(rift.sort(lilly, SortMode.NAME)).isFalse();
        verify(messages).send(lilly, "rift.sort.already", "order", SortMode.NAME.label());
    }

    @Test
    @DisplayName("kept sorted, every deposit lands in its place")
    void autoSort() {
        store.prefs(id, store.prefs(id).withAutoSort(true).withSort(SortMode.NAME));
        rift.deposit(lilly, of(Material.STONE, 5));
        rift.deposit(lilly, of(Material.APPLE, 3));

        assertThat(contents()).containsExactly("APPLEx3", "STONEx5");
    }

    @Test
    @DisplayName("selling takes what the shop buys, and puts everything else back where it was")
    void selling() {
        ItemSales.provide(mock(Plugin.class), buying(Material.DIRT, null));
        rift.deposit(lilly, of(Material.DIRT, 10));
        rift.deposit(lilly, of(Material.DIAMOND_SWORD, 1));
        rift.deposit(lilly, of(Material.DIRT, 64));

        rift.sell(lilly, rift.view(id, Filter.NONE), () -> { });

        assertThat(contents()).containsExactly("DIAMOND_SWORDx1");
    }

    @Test
    @DisplayName("what the shop takes and then does not buy comes back")
    void sellingRefusedAtTheTill() {
        ItemSales.provide(mock(Plugin.class), new ItemBuyer() {
            @Override
            public boolean buys(ItemStack stack) {
                return true;
            }

            @Override
            public CompletableFuture<List<Boolean>> sell(Player seller, List<ItemStack> sold) {
                return CompletableFuture.completedFuture(sold.stream().map(stack -> false).toList());
            }
        });
        rift.deposit(lilly, of(Material.DIRT, 10));
        rift.deposit(lilly, of(Material.STONE, 3));

        rift.sell(lilly, rift.view(id, Filter.NONE), () -> { });

        assertThat(contents()).containsExactlyInAnyOrder("DIRTx10", "STONEx3");
    }

    @Test
    @DisplayName("with no shop on the server, selling says so and touches nothing")
    void noShop() {
        rift.deposit(lilly, of(Material.DIRT, 10));

        rift.sell(lilly, rift.view(id, Filter.NONE), () -> { });

        assertThat(contents()).containsExactly("DIRTx10");
        verify(messages).send(lilly, "rift.sell.no-shop");
    }

    @Test
    @DisplayName("nothing the shop buys is nothing to sell")
    void nothingToSell() {
        ItemSales.provide(mock(Plugin.class), buying(Material.DIRT, null));
        rift.deposit(lilly, of(Material.DIAMOND_SWORD, 1));

        rift.sell(lilly, rift.view(id, Filter.NONE), () -> { });

        assertThat(contents()).containsExactly("DIAMOND_SWORDx1");
        verify(messages).send(lilly, "rift.sell.nothing");
    }

    @Test
    @DisplayName("one sale at a time: a second click while the bank thinks is refused, and nothing is lost")
    void oneSaleAtATime() {
        CompletableFuture<List<Boolean>> thinking = new CompletableFuture<>();
        ItemSales.provide(mock(Plugin.class), buying(Material.DIRT, thinking));
        rift.deposit(lilly, of(Material.DIRT, 10));
        rift.deposit(lilly, of(Material.DIRT, 64));
        AtomicInteger done = new AtomicInteger();

        rift.sell(lilly, rift.view(id, Filter.NONE), done::incrementAndGet);
        assertThat(contents()).as("taken out while the bank decides").isEmpty();
        rift.sell(lilly, rift.view(id, Filter.NONE), done::incrementAndGet);
        verify(messages).send(lilly, "rift.sell.busy");

        // Meanwhile somebody filled the room up: what comes back still comes back.
        stacks.set(1);
        rift.resize(id);
        rift.deposit(lilly, of(Material.STONE, 64));
        thinking.complete(List.of(false, false));

        assertThat(contents()).containsExactlyInAnyOrder("STONEx64", "DIRTx64", "DIRTx10");
        assertThat(done).hasValue(1);
        assertThat(rift.selling(id)).isFalse();
    }

    @Test
    @DisplayName("everything in, from the inventory — the hotbar only when asked")
    void depositAll() {
        ItemStack[] storage = new ItemStack[36];
        storage[0] = of(Material.TORCH, 16);
        storage[9] = of(Material.DIRT, 30);
        for (int slot = 0; slot < 36; slot++) {
            final int at = slot;
            when(inventory.getItem(at)).thenAnswer(ask -> storage[at]);
        }
        org.mockito.Mockito.doAnswer(ask -> {
            storage[ask.<Integer>getArgument(0)] = ask.getArgument(1);
            return null;
        }).when(inventory).setItem(org.mockito.ArgumentMatchers.anyInt(), any());

        rift.depositAll(lilly, false);
        assertThat(contents()).containsExactly("DIRTx30");
        assertThat(storage[0]).isNotNull();

        rift.depositAll(lilly, true);
        assertThat(contents()).containsExactly("DIRTx30", "TORCHx16");
        assertThat(storage[0]).isNull();
        verify(messages, org.mockito.Mockito.times(2)).send(lilly, "rift.stored-all", "count", "1");
    }

    private static ItemBuyer buying(Material only, CompletableFuture<List<Boolean>> answer) {
        return new ItemBuyer() {
            @Override
            public boolean buys(ItemStack stack) {
                return stack.getType() == only;
            }

            @Override
            public CompletableFuture<List<Boolean>> sell(Player seller, List<ItemStack> sold) {
                return answer != null ? answer
                        : CompletableFuture.completedFuture(sold.stream().map(stack -> stack.getType() == only).toList());
            }
        };
    }
}
