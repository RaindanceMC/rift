package de.raindancer.modules.rift.store;

import de.raindancer.core.data.nbt.ItemText;
import de.raindancer.modules.rift.PlainBytes;
import de.raindancer.modules.rift.model.Prefs;
import de.raindancer.modules.rift.model.SortMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RiftStoreTest {

    @TempDir
    Path folder;

    @Test
    @DisplayName("how somebody likes their rift survives a restart; nobody else's is touched")
    void prefs() {
        UUID lilly = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        RiftStore store = new RiftStore(folder, new ItemText(new PlainBytes()));
        assertThat(store.prefs(lilly)).isEqualTo(Prefs.DEFAULTS);

        store.prefs(lilly, new Prefs(SortMode.AMOUNT, true, 4));

        RiftStore again = new RiftStore(folder, new ItemText(new PlainBytes()));
        assertThat(again.prefs(lilly)).as("the fragments already told are kept too")
                .isEqualTo(new Prefs(SortMode.AMOUNT, true, 4));
        assertThat(again.prefs(other)).isEqualTo(Prefs.DEFAULTS);
    }

    @Test
    @DisplayName("each rift is its own file under rifts/")
    void layout() {
        UUID lilly = UUID.randomUUID();
        RiftStore store = new RiftStore(folder, new ItemText(new PlainBytes()));
        var rift = store.load(lilly, 36);
        rift.deposit(de.raindancer.core.testkit.TestItems.of(org.bukkit.Material.DIRT, 3));

        assertThat(store.save(lilly, rift.contents())).isTrue();
        assertThat(Files.exists(folder.resolve("rifts").resolve(lilly + ".yml"))).isTrue();
    }
}
