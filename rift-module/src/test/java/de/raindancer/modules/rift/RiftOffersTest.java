package de.raindancer.modules.rift;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.rift.model.Prefs;
import de.raindancer.modules.rift.model.SortMode;
import de.raindancer.modules.rift.service.RiftService;
import de.raindancer.modules.rift.util.PermissionNodes;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiftOffersTest {

    private final RiftService rift = mock(RiftService.class);
    private final Player player = mock(Player.class);
    private final UUID id = UUID.randomUUID();

    RiftOffersTest() {
        when(player.getUniqueId()).thenReturn(id);
    }

    @Test
    @DisplayName("sorting is a choice between the readable sort names, showing the one set now")
    void sortChoice() {
        Preference preference = RiftOffers.sort(rift);
        when(rift.prefs(id)).thenReturn(new Prefs(SortMode.NAME, false));

        assertThat(preference.id()).isEqualTo("rift:sort");
        assertThat(preference.title()).isEqualTo("Sort your rift by");
        assertThat(preference.options()).containsExactly("by kind", "A to Z", "biggest stacks first");
        assertThat(preference.state(player)).isEqualTo("A to Z");
    }

    @Test
    @DisplayName("choosing a sort saves it, and sorts the rift at once only when it is kept sorted")
    void sortChosen() {
        Preference preference = RiftOffers.sort(rift);
        when(rift.prefs(id)).thenReturn(new Prefs(SortMode.KIND, false));

        preference.choose().accept(player, 2);
        verify(rift).prefs(id, new Prefs(SortMode.AMOUNT, false));
        verify(rift, never()).sort(any(), any());

        when(rift.prefs(id)).thenReturn(new Prefs(SortMode.KIND, true));
        preference.choose().accept(player, 1);
        verify(rift).prefs(id, new Prefs(SortMode.NAME, true));
        verify(rift).sort(player, SortMode.NAME);
    }

    @Test
    @DisplayName("keeping it sorted is a switch; turning it on sorts the rift now, turning it off only saves")
    void autoSort() {
        Preference preference = RiftOffers.autoSort(rift);
        when(rift.prefs(id)).thenReturn(new Prefs(SortMode.NAME, false));

        assertThat(preference.id()).isEqualTo("rift:auto-sort");
        assertThat(preference.title()).isEqualTo("Sort your rift on its own");
        assertThat(preference.state(player)).isEqualTo("Off");

        preference.choose().accept(player, 0);
        verify(rift).prefs(id, new Prefs(SortMode.NAME, true));
        verify(rift).sort(player, SortMode.NAME);

        when(rift.prefs(id)).thenReturn(new Prefs(SortMode.NAME, true));
        assertThat(preference.state(player)).isEqualTo("On");
        preference.choose().accept(player, 1);
        verify(rift).prefs(id, new Prefs(SortMode.NAME, false));
        verify(rift, org.mockito.Mockito.times(1)).sort(any(), any());
    }

    @Test
    @DisplayName("both are offered only to whoever may use a rift")
    void gated() {
        for (Preference preference : new Preference[]{RiftOffers.sort(rift), RiftOffers.autoSort(rift)}) {
            when(player.hasPermission(PermissionNodes.USE)).thenReturn(false);
            assertThat(preference.shown().test(player)).isFalse();
            when(player.hasPermission(PermissionNodes.USE)).thenReturn(true);
            assertThat(preference.shown().test(player)).isTrue();
        }
    }
}
