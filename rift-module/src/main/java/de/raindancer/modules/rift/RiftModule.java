package de.raindancer.modules.rift;

import de.raindancer.core.data.nbt.ItemText;
import de.raindancer.core.data.settings.SettingsStore;
import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.core.platform.util.Cooldowns;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.social.economy.BuyableSlots;
import de.raindancer.modules.api.FlexModule;
import de.raindancer.modules.api.ModuleCommand;
import de.raindancer.modules.api.ModuleContext;
import de.raindancer.modules.api.ModuleInfo;
import de.raindancer.modules.rift.listener.RiftPlayerListener;
import de.raindancer.modules.rift.model.Filter;
import de.raindancer.modules.rift.screen.RiftMenu;
import de.raindancer.modules.rift.service.PageService;
import de.raindancer.modules.rift.service.RiftService;
import de.raindancer.modules.rift.store.RiftStore;
import de.raindancer.modules.rift.util.PermissionNodes;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * A pocket dimension every player can open: their own vault of pages that grows as they play.
 *
 * <p>Shipped through the standard wrapper this is {@code RainsRift}. Each rift is a Core stash without armour stands,
 * one page per 36 stacks: some to start with, one free page per so many hours of active playtime, more bought with
 * money (Core's {@code BuyableSlots}), a bonus by {@code rains.rift.pages.<n>}. It can be searched, narrowed to one kind
 * of item, sorted once or kept sorted, and a page — or everything shown — sold to the shop through Core's
 * {@code ItemSales}, which the economy module answers.
 *
 * <h2>The one invariant</h2>
 * Nothing is duplicated and nothing is lost. The window shows copies; every move goes through the stash, which hands
 * a stack out only while it is still what the screen showed. A sale takes the stacks out first and puts back every one
 * the shop did not buy, even into a rift that filled up meanwhile.
 */
public final class RiftModule implements FlexModule {

    private static final ModuleInfo INFO = ModuleInfo.of("rift", "Rift", "0.2.2")
            .describedAs("A pocket dimension for your things: a vault of pages that grows with playtime and money, "
                    + "with search, kinds, sorting and selling.")
            .by("Raindancer118");

    /** A minute: often enough that a page earned by playtime arrives while the player still remembers waiting. */
    private static final long GROWTH_CHECK_TICKS = 20L * 60;

    private LogChannel log;
    private RiftServices services;

    @Override
    public ModuleInfo info() {
        return INFO;
    }

    @Override
    public void enable(ModuleContext context) {
        log = context.log();
        Plugin plugin = context.plugin();
        SettingsStore<RiftSettings> settings = context.settings(RiftSettings.class, RiftSettings.DEFAULTS);

        context.core().messages().defineFrom(RiftModule.class.getResourceAsStream("messages.yml"),
                context.chat().brand()::chatPrefix);
        context.core().messages().seriousFrom(RiftModule.class.getResourceAsStream("messages-serious.yml"));

        int registered = PermissionNodes.register(plugin.getServer());
        if (registered > 0) {
            log.info("{} permission(s) registered.", registered);
        }

        BuyableSlots bought = new BuyableSlots(context.dataFolder().resolve("bought-pages.yml"), "rift.page",
                "Rift page");
        bought.load();
        PageService pages = new PageService(settings::current,
                id -> context.core().playtime().playedActively(id).toMinutes(), bought);
        RiftService rift = new RiftService(new RiftStore(context.dataFolder(), ItemText.ofTheServer()),
                context.core().messages(), id -> pages.pages(id).stacks(), new LiveThreads(plugin));

        Cooldowns<UUID> sellWait = new Cooldowns<>();
        sellWait.every(Duration.ofSeconds(Math.max(0, settings.current().sellWaitSeconds())));
        settings.onChange(fresh -> {
            sellWait.every(Duration.ofSeconds(Math.max(0, fresh.sellWaitSeconds())));
            rift.resizeAll();
        });

        services = new RiftServices(plugin, plugin.getServer(), log, context.core().messages(), context.chat().brand(),
                context.core(), settings::current, settings, rift, pages, sellWait, new LiveScreens());

        context.listener(new RiftPlayerListener(services));
        RiftCommands.ready(services);

        var growth = Scheduling.globalTimer(plugin, GROWTH_CHECK_TICKS, GROWTH_CHECK_TICKS, task -> {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                Scheduling.entity(plugin, online, () -> pages.noticed(online).ifPresent(total -> {
                    rift.resize(online.getUniqueId());
                    services.messages().send(online, "rift.page.grew", "pages", String.valueOf(total));
                }));
            }
        });
        if (growth != null) {
            context.closeWith(growth::cancel);
        }
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            Scheduling.entity(plugin, online, () -> pages.noticed(online));
        }

        RiftSettings live = settings.current();
        log.info("Rifts are open: {} page(s) to start, at most {}, a free one every {} hour(s) played (up to {}), "
                        + "buying {}, selling {}.", live.startingPages(), live.mostPagesClamped(),
                live.freePageEveryHours(), live.mostFreePages(), live.pagesForSale() ? "on" : "off",
                live.sellingOn() ? "on" : "off");
    }

    private final class LiveScreens implements IRiftScreensOpener {

        @Override
        public void rift(Player viewer, String search) {
            new RiftMenu(services, viewer, null, Filter.NONE.searching(search)).open();
        }
    }

    /** The player's own region for their inventory, Core's async pool for the disk. */
    private record LiveThreads(Plugin plugin) implements RiftService.Threads {

        @Override
        public void onPlayer(Player player, Runnable task) {
            Scheduling.entity(plugin, player, task);
        }

        @Override
        public void offThread(Runnable task) {
            Scheduling.async(plugin, task);
        }
    }

    @Override
    public List<ModuleCommand> commands() {
        return RiftCommands.declared();
    }

    /** Every rift is written before anything is torn down: the async saves have no later after this. */
    @Override
    public void disable() {
        if (services != null && !services.rift().flushNow()) {
            log.error("Not every rift could be written while stopping. What was last saved is still on disk.");
        }
        RiftCommands.stopped();
    }
}
