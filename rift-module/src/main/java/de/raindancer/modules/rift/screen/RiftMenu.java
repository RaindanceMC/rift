package de.raindancer.modules.rift.screen;

import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.social.economy.BuyableSlots;
import de.raindancer.core.social.economy.Fees;
import de.raindancer.core.social.economy.Money;
import de.raindancer.core.ui.effect.Cues;
import de.raindancer.core.ui.menu.ConfirmMenu;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.MenuLayout;
import de.raindancer.core.ui.prompt.AnvilInput;
import de.raindancer.core.ui.prompt.Parsers;
import de.raindancer.core.ui.text.Text;
import de.raindancer.modules.rift.RiftServices;
import de.raindancer.modules.rift.model.Entry;
import de.raindancer.modules.rift.model.Filter;
import de.raindancer.modules.rift.model.Pages;
import de.raindancer.modules.rift.model.Prefs;
import de.raindancer.modules.rift.util.PermissionNodes;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@code /rift} — a player's own pocket dimension: pages of stacks, a search, a kind filter, sorting and selling.
 *
 * <p>Nothing in the window is a real item: the slots show copies of what the rift holds, and every click asks the rift
 * to move something. That is why shift-clicking in from below, dropping the cursor's item onto the page and clicking an
 * entry are each handled here rather than left to vanilla — vanilla would move the copies, and a copy that can be picked
 * up is a duplicated item.
 */
public final class RiftMenu extends Menu implements IRiftScreen {

    /** The clicks that mean "take this". Number keys, Q and middle click on an entry are ignored. */
    private static final Set<ClickType> TAKING =
            Set.of(ClickType.LEFT, ClickType.RIGHT, ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT);

    private static final int PER_PAGE = Pages.STACKS_PER_PAGE;

    private final RiftServices services;
    private Filter filter;
    private int page;
    private List<Entry> shown = List.of();

    public RiftMenu(RiftServices services, Player viewer, Menu parent, Filter filter) {
        super(viewer, services.brand(), parent);
        this.services = services;
        this.filter = filter == null ? Filter.NONE : filter;
    }

    @Override
    protected Component title() {
        return Component.text(filter.isActive() ? "Rift — searching" : "Rift");
    }

    @Override
    public String breadcrumb() {
        return "your rift";
    }

    @Override
    public String describe() {
        return "a player's rift: pages, search, sorting and selling";
    }

    private Pages pages() {
        return services.pages().pages(viewer);
    }

    private int pageCount() {
        int needed = MenuLayout.pageCount(Math.max(1, shown.size()), PER_PAGE);
        return filter.isActive() ? needed : Math.max(needed, pages().total());
    }

    private List<Entry> onThisPage() {
        int from = page * PER_PAGE;
        return shown.subList(Math.min(from, shown.size()), Math.min(from + PER_PAGE, shown.size()));
    }

    @Override
    protected void render() {
        services.rift().resize(viewer.getUniqueId());
        shown = services.rift().view(viewer.getUniqueId(), filter);
        page = MenuLayout.clampPage(page, pageCount());
        List<Entry> here = onThisPage();
        for (int slot = 0; slot < here.size(); slot++) {
            Entry entry = here.get(slot);
            set(slot, entry.shown().clone(), event -> {
                if (!stillAllowed() || !TAKING.contains(event.getClick())) {
                    return;
                }
                services.rift().take(viewer, entry);
                refresh();
            });
        }
    }

    @Override
    protected void decorate() {
        Prefs prefs = services.rift().prefs(viewer.getUniqueId());
        List<Entry> here = onThisPage();

        toolbar(1, search(), event -> {
            if (event.isShiftClick()) {
                showing(filter.searching(""));
                return;
            }
            AnvilInput.open(viewer, "Search your rift", filter.query(), Parsers.text(40),
                    typed -> {
                        filter = filter.searching(typed);
                        page = 0;
                        reopen();
                    }, this::reopen);
        });
        toolbar(2, kind(), event -> {
            if (event.isShiftClick()) {
                showing(filter.ofKind(null));
            } else if (event.isRightClick()) {
                showing(filter.previousKind());
            } else {
                showing(filter.nextKind());
            }
        });
        toolbar(3, Icons.of(prefs.autoSort() ? Material.COMPARATOR : Material.REPEATER, "<green>Sort",
                        "<gray>Order: <white>" + prefs.sort().label(),
                        "<gray>Kept sorted: " + (prefs.autoSort() ? "<green>on" : "<red>off"),
                        "",
                        "<gray>Click: sort now",
                        "<gray>Right click: another order",
                        "<gray>Shift-click: keep it sorted, on or off"),
                event -> {
                    if (!stillAllowed()) {
                        return;
                    }
                    if (event.isShiftClick()) {
                        services.rift().prefs(viewer.getUniqueId(), prefs.withAutoSort(!prefs.autoSort()));
                        if (!prefs.autoSort()) {
                            services.rift().sort(viewer, prefs.sort());
                        }
                    } else if (event.isRightClick()) {
                        services.rift().prefs(viewer.getUniqueId(), prefs.withSort(prefs.sort().next()));
                        if (prefs.autoSort()) {
                            services.rift().sort(viewer, prefs.sort().next());
                        }
                    } else {
                        services.rift().sort(viewer, prefs.sort());
                    }
                    refresh();
                });
        toolbar(4, Icons.of(Material.CHEST, "<green>Put everything in",
                        "<gray>Click: your inventory, not the hotbar",
                        "<gray>Shift-click: the hotbar too",
                        "<dark_gray>What you wear stays on.",
                        "<dark_gray>What does not fit stays with you."),
                event -> {
                    if (stillAllowed()) {
                        services.rift().depositAll(viewer, event.isShiftClick());
                        refresh();
                    }
                });
        toolbar(5, !here.isEmpty(), Icons.of(Material.HOPPER, "<green>Take out",
                        "<gray>Click: everything on this page",
                        "<gray>Shift-click: everything " + (filter.isActive() ? "found" : "in the rift"),
                        "<dark_gray>What does not fit stays in here."),
                "Nothing here to take",
                event -> {
                    if (stillAllowed()) {
                        services.rift().takeAll(viewer, event.isShiftClick() ? shown : here);
                        refresh();
                    }
                });
        sellButton(here);
        pagesButton();
        fillRow(MenuLayout.TOOLBAR_ROW, Material.BLACK_STAINED_GLASS_PANE);
    }

    private ItemStack search() {
        if (!filter.isSearching()) {
            return Icons.of(Material.COMPASS, "<green>Search",
                    "<gray>Click: type what you are looking for",
                    "<dark_gray>Names, given names, enchantments.");
        }
        return Icons.of(Material.RECOVERY_COMPASS, "<green>Search: <white>" + Text.literal(filter.query()),
                "<gray>" + shown.size() + " stack(s) found",
                "",
                "<gray>Click: search for something else",
                "<gray>Shift-click: stop searching");
    }

    private ItemStack kind() {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Showing: <white>" + (filter.kind() == null ? "everything" : filter.kind().title()));
        lore.add("");
        lore.add("<gray>Click: the next kind");
        lore.add("<gray>Right click: the one before");
        lore.add("<gray>Shift-click: everything again");
        Material icon = filter.kind() == null ? Material.HOPPER_MINECART
                : Optional.ofNullable(Material.matchMaterial(filter.kind().icon())).orElse(Material.HOPPER_MINECART);
        return Icons.of(icon, "<green>Kind", lore);
    }

    private void sellButton(List<Entry> here) {
        boolean on = services.config().sellingOn() && viewer.hasPermission(PermissionNodes.SELL);
        boolean any = on && services.rift().anythingSellable(shown);
        String reason = !on ? "Selling from the rift is off here"
                : services.rift().selling(viewer.getUniqueId()) ? "A sale is still being settled"
                : "Nothing here the shop buys";
        toolbar(6, any && !services.rift().selling(viewer.getUniqueId()),
                Icons.of(Material.EMERALD, "<green>Sell",
                        "<gray>Click: what the shop buys on this page",
                        "<gray>Shift-click: everything " + (filter.isActive() ? "found" : "in the rift"),
                        "<gray>   — asks first",
                        "<dark_gray>Plain items only, at your /sell prices.",
                        "<dark_gray>Anything else stays in the rift."),
                reason,
                event -> {
                    if (!stillAllowed() || !waited()) {
                        return;
                    }
                    if (!event.isShiftClick()) {
                        sell(here);
                        return;
                    }
                    List<Entry> everything = shown;
                    new ConfirmMenu(viewer, services.brand(), this,
                            "<gold>Sell everything " + (filter.isActive() ? "found" : "in your rift") + "?",
                            List.of("<gray>Everything plain the shop buys among",
                                    "<gray>" + everything.size() + " stack(s) goes, at your /sell prices.",
                                    "<gray>Everything else stays."),
                            () -> sell(everything)).open();
                });
    }

    private void sell(List<Entry> entries) {
        services.rift().sell(viewer, entries, () -> {
            if (viewer.getOpenInventory().getTopInventory().getHolder() == this) {
                refresh();
            } else {
                reopen();
            }
        });
        refresh();
    }

    private boolean waited() {
        if (services.sellWait().tryUse(viewer.getUniqueId())) {
            return true;
        }
        services.messages().send(viewer, "rift.sell.wait");
        return false;
    }

    private void pagesButton() {
        Pages pages = pages();
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Pages: <white>" + pages.total() + "<gray> of at most <white>"
                + services.config().mostPagesClamped());
        lore.add("<gray>Stacks: <white>" + services.rift().of(viewer.getUniqueId()).size() + "<gray>/<white>"
                + pages.stacks());
        lore.add("");
        lore.add("<dark_gray>" + pages.starting() + " to start, " + pages.free() + " for playtime, "
                + pages.bought() + " bought" + (pages.bonus() > 0 ? ", " + pages.bonus() + " bonus" : ""));
        pages.minutesToNextFree().ifPresent(minutes ->
                lore.add("<gray>Next free page in <white>" + hours(minutes) + "<gray> played"));
        Optional<Money> price = services.pages().nextPrice(viewer.getUniqueId());
        price.ifPresent(cost -> {
            lore.add("");
            lore.add("<gray>Click: buy page " + (pages.total() + 1) + " for <white>" + Fees.format(cost));
        });
        if (pages.full()) {
            lore.add("<green>As big as a rift gets.");
        }
        toolbar(7, Icons.of(Material.ENDER_EYE, "<light_purple>Pages", lore), event -> {
            if (price.isEmpty() || !stillAllowed()) {
                if (price.isEmpty()) {
                    services.messages().send(viewer, pages.full() ? "rift.page.maxed" : "rift.page.not-for-sale");
                }
                return;
            }
            new ConfirmMenu(viewer, services.brand(), this,
                    "<light_purple>Buy rift page " + (pages.total() + 1) + "?",
                    List.of("<gray>" + Pages.STACKS_PER_PAGE + " more stacks, for <white>" + Fees.format(price.get())),
                    "<dark_gray>Bought pages are yours for good.",
                    () -> buy(pages.total() + 1)).open();
        });
    }

    private void buy(int number) {
        services.pages().buy(viewer.getUniqueId(), task -> Scheduling.async(services.plugin(), task))
                .thenAccept(purchase -> Scheduling.entity(services.plugin(), viewer, () -> {
                    if (purchase.outcome() == BuyableSlots.Outcome.BOUGHT) {
                        services.rift().resize(viewer.getUniqueId());
                        services.effects().play(viewer.getUniqueId(), Cues.REWARD);
                        services.messages().send(viewer, "rift.page.bought", "page", String.valueOf(number),
                                "price", Fees.format(purchase.paid()));
                    } else {
                        services.effects().play(viewer.getUniqueId(), Cues.NO);
                        services.messages().send(viewer, "rift.page." + outcomeKey(purchase.outcome()));
                    }
                    reopen();
                }));
    }

    private static String outcomeKey(BuyableSlots.Outcome outcome) {
        return switch (outcome) {
            case BOUGHT -> "bought";
            case OFF, NO_PRICE -> "not-for-sale";
            case MAXED -> "maxed";
            case NOT_ENOUGH -> "not-enough";
            case NO_ECONOMY -> "no-economy";
            case REFUSED -> "refused";
            case NOT_SAVED -> "not-saved";
        };
    }

    private static String hours(long minutes) {
        return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + "m";
    }

    private void showing(Filter changed) {
        filter = changed;
        page = 0;
        reopen();
    }

    @Override
    protected void paintPagingChrome(int chromeRow) {
        int pages = pageCount();
        if (page > 0) {
            set(chromeRow + MenuLayout.CHROME_PREVIOUS, Icons.previousPage(page, pages), event -> turnTo(page - 1));
        }
        if (page < pages - 1) {
            set(chromeRow + MenuLayout.CHROME_NEXT, Icons.nextPage(page + 2, pages), event -> turnTo(page + 1));
        }
        set(chromeRow + MenuLayout.CHROME_PAGE, Icons.pageCounter(page + 1, pages));
    }

    private void turnTo(int newPage) {
        page = newPage;
        refresh();
    }

    @Override
    protected List<String> helpLines() {
        return List.of(
                "<gray>Shift-click anything in your inventory",
                "<gray>to put it in, or drop it onto a page.",
                "<gray>Click a stack to take it out.",
                "<gray>Search and Kind narrow what is shown;",
                "<gray>Take out and Sell work on what is shown.",
                "<gray>Pages come with playtime, for money,",
                "<gray>or as a rank's bonus.");
    }

    /** A click with something on the cursor, anywhere on the pages, puts it in. */
    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack cursor = event.getCursor();
        if (slot >= 0 && slot < PER_PAGE && !cursor.isEmpty()) {
            event.setCancelled(true);
            if (!stillAllowed()) {
                return;
            }
            int accepted = services.rift().deposit(viewer, cursor);
            if (accepted < cursor.getAmount()) {
                services.messages().send(viewer, "rift.full");
            }
            cursor.setAmount(cursor.getAmount() - accepted);
            viewer.setItemOnCursor(cursor.isEmpty() ? null : cursor);
            refresh();
            return;
        }
        super.handleClick(event);
    }

    @Override
    public boolean allowBottomInventoryInteraction() {
        return true;
    }

    /**
     * Their own inventory: shift-click puts the stack in, everything else works as it always does — except a double
     * click, which would gather matching copies off the page onto the cursor.
     */
    @Override
    public void handleBottomClick(InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (!event.isShiftClick()) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.isEmpty() || !stillAllowed()) {
            return;
        }
        int accepted = services.rift().deposit(viewer, clicked);
        if (accepted < clicked.getAmount()) {
            services.messages().send(viewer, "rift.full");
        }
        clicked.setAmount(clicked.getAmount() - accepted);
        event.setCurrentItem(clicked.isEmpty() ? null : clicked);
        refresh();
    }

    /** Asked again on every click: the window can stay open after the node is taken away. */
    private boolean stillAllowed() {
        if (viewer.hasPermission(PermissionNodes.USE)) {
            return true;
        }
        services.messages().send(viewer, "rift.not-allowed");
        viewer.closeInventory();
        return false;
    }
}
