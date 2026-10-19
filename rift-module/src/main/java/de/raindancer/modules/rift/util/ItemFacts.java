package de.raindancer.modules.rift.util;

import de.raindancer.core.ui.choose.Catalogue;
import de.raindancer.modules.rift.model.Searchable;
import de.raindancer.modules.rift.model.SortKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** What a search and a sort read off a stack. */
public final class ItemFacts {

    private ItemFacts() {
    }

    public static Searchable searchable(ItemStack item) {
        List<String> extra = new ArrayList<>();
        String custom = null;
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;
        if (meta != null) {
            if (meta.hasCustomName()) {
                custom = plain(meta.customName());
            }
            enchantments(meta.getEnchants(), extra);
            if (meta instanceof EnchantmentStorageMeta book) {
                enchantments(book.getStoredEnchants(), extra);
            }
            if (meta instanceof PotionMeta potion && potion.getBasePotionType() != null) {
                extra.add(Catalogue.readable(potion.getBasePotionType().getKey().getKey()));
            }
            List<Component> lore = meta.lore();
            if (lore != null) {
                lore.forEach(line -> extra.add(plain(line)));
            }
        }
        String material = item.getType().name();
        return new Searchable(Catalogue.readable(material), custom, extra, Catalogue.categoryOf(material));
    }

    /** Sorted by what a player reads: the given name when there is one, otherwise the material's. */
    public static SortKey sortKey(ItemStack item) {
        Searchable facts = searchable(item);
        String name = facts.customName() != null ? facts.customName() : facts.material();
        return new SortKey(facts.kind(), name, item.getAmount());
    }

    private static void enchantments(Map<Enchantment, Integer> enchants, List<String> into) {
        enchants.keySet().forEach(enchantment -> into.add(Catalogue.readable(enchantment.getKey().getKey())));
    }

    private static String plain(Component text) {
        return text == null ? "" : PlainTextComponentSerializer.plainText().serialize(text);
    }
}
