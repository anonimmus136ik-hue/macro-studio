package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Inventory-only resource filter. Does not search the world or hidden blocks. */
public final class ResourceCatalog {
    public static final String[] NAMES = {"DIAMOND", "EMERALD", "GOLD", "IRON", "REDSTONE", "COAL", "STONE"};
    private static final Item[] ITEMS = {
            Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT,
            Items.REDSTONE, Items.COAL, Items.COBBLESTONE
    };
    private ResourceCatalog() {}

    public static int count(Minecraft mc, int type) {
        if (mc == null || mc.player == null || type < 0 || type >= ITEMS.length) return 0;
        Item item = ITEMS[type];
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    public static boolean selected(int type) {
        boolean[] filter = Settings.I.resourceFilter;
        return filter != null && type >= 0 && type < filter.length && filter[type];
    }
}
