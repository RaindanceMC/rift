package de.raindancer.modules.rift.model;

import org.bukkit.inventory.ItemStack;

/**
 * One stack as a screen shows it: where it is in the rift and a copy of what it was. Taking it only works while the
 * rift still holds exactly that there.
 */
public record Entry(int index, ItemStack shown) {
}
