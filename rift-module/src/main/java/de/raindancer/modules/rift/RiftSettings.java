package de.raindancer.modules.rift;

import de.raindancer.core.data.settings.Describe;
import de.raindancer.core.data.settings.In;
import de.raindancer.core.data.settings.Key;
import de.raindancer.core.data.settings.Range;
import de.raindancer.core.data.settings.Settings;
import de.raindancer.core.data.settings.Title;
import de.raindancer.core.data.settings.Topic;
import de.raindancer.core.social.economy.BuyableSlots;
import de.raindancer.core.social.economy.Fees;
import de.raindancer.modules.rift.model.PageTerms;
import org.bukkit.Material;

/**
 * What an owner can decide about rifts. The record is the schema: the file, its comments, its validation and the
 * {@code /settings} screens all come from it.
 */
@Settings(id = "rift", topics = {
        @Topic(path = "rift", title = "Rift", icon = Material.ENDER_CHEST),
        @Topic(path = "rift/pages", title = "Pages", icon = Material.BOOK),
        @Topic(path = "rift/selling", title = "Selling", icon = Material.EMERALD),
})
public record RiftSettings(

        @In("rift/pages") @Title("Pages to start with") @Range(min = 1, max = 100)
        @Describe("How many pages of 36 stacks every rift has from the first time it is opened.")
        @Key("pages.starting")
        int startingPages,

        @In("rift/pages") @Title("Most pages") @Range(min = 1, max = 100)
        @Describe("The most pages one rift can ever have, however they were earned, bought or granted. Lowering "
                + "this never deletes anything: a rift over the limit keeps what it holds and takes nothing more "
                + "until it is under again.")
        @Key("pages.most")
        int mostPages,

        @In("rift/pages") @Title("A free page every … hours played") @Range(min = 0, max = 10000)
        @Describe("Hours of active playtime (time away does not count) per free page. 0 switches free pages off.")
        @Key("pages.free-every-hours")
        int freePageEveryHours,

        @In("rift/pages") @Title("Most free pages") @Range(min = 0, max = 100)
        @Describe("How many pages playtime can earn in all. Past that, pages are bought or granted.")
        @Key("pages.most-free")
        int mostFreePages,

        @In("rift/pages") @Title("Pages can be bought")
        @Describe("Whether players can buy more pages with money. Needs an economy plugin.")
        @Key("pages.buying")
        boolean pagesForSale,

        @In("rift/pages") @Title("The first bought page costs")
        @Describe("Money, such as 5000 or 5k. Each further page costs the percentage below more.")
        @Key("pages.price")
        String pagePrice,

        @In("rift/pages") @Title("Each bought page costs this much more") @Range(min = 0, max = 1000)
        @Describe("Percent on top of the one before, so with 100 every page costs double the one before.")
        @Key("pages.price-growth-percent")
        int pagePriceGrowthPercent,

        @In("rift/selling") @Title("Selling from the rift")
        @Describe("Whether the rift has Sell this page and Sell everything shown. They sell to the shop at the "
                + "player's own prices — only plain items, the way /sell does — and need the economy module.")
        @Key("selling.enabled")
        boolean sellingOn,

        @In("rift/selling") @Title("Wait between sales") @Range(min = 0, max = 60)
        @Describe("Seconds between two sales by the same player, so a held-down click cannot flood the shop.")
        @Key("selling.wait-seconds")
        int sellWaitSeconds) {

    public static final RiftSettings DEFAULTS = new RiftSettings(1, 12, 6, 3, true, "5000", 100, true, 2);

    public int mostPagesClamped() {
        return Math.max(1, Math.min(100, mostPages));
    }

    public PageTerms pageTerms() {
        return new PageTerms(startingPages, mostPagesClamped(), freePageEveryHours, mostFreePages);
    }

    /**
     * What buying a page costs, as Core's {@link BuyableSlots} reads it. No cap of its own: the page rule stops
     * buying once the most pages are reached.
     */
    public BuyableSlots.Terms pageBuying() {
        return new BuyableSlots.Terms(pagesForSale, Fees.amount(pagePrice), Math.max(0, pagePriceGrowthPercent), 0);
    }

    public RiftSettings withStartingPages(int value) {
        return new RiftSettings(value, mostPages, freePageEveryHours, mostFreePages, pagesForSale, pagePrice,
                pagePriceGrowthPercent, sellingOn, sellWaitSeconds);
    }

    public RiftSettings withMostPages(int value) {
        return new RiftSettings(startingPages, value, freePageEveryHours, mostFreePages, pagesForSale, pagePrice,
                pagePriceGrowthPercent, sellingOn, sellWaitSeconds);
    }

    public RiftSettings withFreePageEveryHours(int value) {
        return new RiftSettings(startingPages, mostPages, value, mostFreePages, pagesForSale, pagePrice,
                pagePriceGrowthPercent, sellingOn, sellWaitSeconds);
    }

    public RiftSettings withPagePrice(String value) {
        return new RiftSettings(startingPages, mostPages, freePageEveryHours, mostFreePages, pagesForSale, value,
                pagePriceGrowthPercent, sellingOn, sellWaitSeconds);
    }

    public RiftSettings withSellingOn(boolean value) {
        return new RiftSettings(startingPages, mostPages, freePageEveryHours, mostFreePages, pagesForSale, pagePrice,
                pagePriceGrowthPercent, value, sellWaitSeconds);
    }
}
