package de.raindancer.modules.rift;

import de.raindancer.core.social.economy.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiftSettingsTest {

    private final RiftSettings defaults = RiftSettings.DEFAULTS;

    @Test
    @DisplayName("out of the box: one page, twelve at most, free pages for playtime, buying and selling on")
    void defaults() {
        assertThat(defaults.pageTerms().starting()).isEqualTo(1);
        assertThat(defaults.mostPagesClamped()).isEqualTo(12);
        assertThat(defaults.pageTerms().freeEveryHours()).isPositive();
        assertThat(defaults.pagesForSale()).isTrue();
        assertThat(defaults.sellingOn()).isTrue();
    }

    @Test
    @DisplayName("the page price is read in the server's money; buying has no cap of its own")
    void buyingTerms() {
        assertThat(defaults.withPagePrice("2.5k").pageBuying().price()).isEqualTo(Money.of(250000));
        assertThat(defaults.pageBuying().mostPerPlayer()).isZero();
        assertThat(defaults.withPagePrice("nonsense").pageBuying().price()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("pages are dear: the first bought one is 5,000 and each after costs double")
    void pagesAreDear() {
        var terms = defaults.pageBuying();

        assertThat(terms.price()).isEqualTo(de.raindancer.core.social.economy.Fees.amount("5000"));
        assertThat(terms.growthPercent()).isEqualTo(100);
        assertThat(de.raindancer.core.social.economy.BuyableSlots.priceOfNext(terms.price(), terms.growthPercent(), 7))
                .as("the eighth bought page")
                .isEqualTo(Money.of(terms.price().minor() * 128));
    }

    @Test
    @DisplayName("nonsense in the file is clamped rather than believed")
    void clamped() {
        assertThat(defaults.withMostPages(0).mostPagesClamped()).isEqualTo(1);
        assertThat(defaults.withMostPages(5000).mostPagesClamped()).isEqualTo(100);
    }
}
