package de.raindancer.modules.rift.store;

import de.raindancer.core.data.nbt.ItemText;
import de.raindancer.core.data.stash.Stash;
import de.raindancer.core.data.stash.StashStore;
import de.raindancer.core.data.store.YamlStore;
import de.raindancer.modules.rift.model.Prefs;
import de.raindancer.modules.rift.model.SortMode;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rifts on disk: each one a Core stash without armour stands under {@code rifts/<uuid>.yml}, and everybody's sort
 * preferences in {@code prefs.yml}. This server's own, like every saved inventory.
 */
public final class RiftStore {

    private final StashStore stashes;
    private final YamlStore prefsFile;
    private final Map<UUID, Prefs> prefs = new ConcurrentHashMap<>();
    private volatile boolean prefsRead;

    public RiftStore(Path folder, ItemText codec) {
        this.stashes = new StashStore(folder.resolve("rifts"), codec);
        this.prefsFile = new YamlStore(folder.resolve("prefs.yml"));
    }

    public Stash load(UUID owner, int stacks) {
        return stashes.loadWithoutStands(owner, stacks);
    }

    /** @return whether the disk holds this picture or a newer one */
    public boolean save(UUID owner, Stash.Contents contents) {
        return stashes.save(owner, contents);
    }

    public Prefs prefs(UUID player) {
        readPrefs();
        return prefs.getOrDefault(player, Prefs.DEFAULTS);
    }

    /** Remembers and writes at once — preferences change by a click, rarely. */
    public boolean prefs(UUID player, Prefs changed) {
        readPrefs();
        prefs.put(player, changed);
        return prefsFile.update(yaml -> {
            yaml.set(player + ".sort", changed.sort().key());
            yaml.set(player + ".auto-sort", changed.autoSort());
        });
    }

    private void readPrefs() {
        if (prefsRead) {
            return;
        }
        synchronized (this) {
            if (prefsRead) {
                return;
            }
            YamlConfiguration yaml = prefsFile.read();
            for (String key : yaml.getKeys(false)) {
                try {
                    prefs.put(UUID.fromString(key), new Prefs(SortMode.parse(yaml.getString(key + ".sort")),
                            yaml.getBoolean(key + ".auto-sort")));
                } catch (IllegalArgumentException notAnId) {
                    // Only ever written here; skipped rather than trusted.
                }
            }
            prefsRead = true;
        }
    }
}
