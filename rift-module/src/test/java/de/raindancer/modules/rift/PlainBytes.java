package de.raindancer.modules.rift;

import de.raindancer.core.data.nbt.ItemBytes;
import de.raindancer.core.testkit.TestItems;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** "MATERIAL:amount" as bytes — all the store tests need an item to be on disk. */
public final class PlainBytes implements ItemBytes {

    @Override
    public int dataVersion() {
        return 1;
    }

    @Override
    public byte[] toBytes(ItemStack item) {
        return (item.getType().name() + ":" + item.getAmount()).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean isNothing(ItemStack item) {
        return item == null || item.isEmpty();
    }

    @Override
    public Optional<ItemStack> fromBytes(byte[] bytes) {
        String[] parts = new String(bytes, StandardCharsets.UTF_8).split(":");
        Material material = parts.length == 2 ? Material.matchMaterial(parts[0]) : null;
        if (material == null) {
            return Optional.empty();
        }
        return Optional.of(TestItems.of(material, Integer.parseInt(parts[1])));
    }
}
