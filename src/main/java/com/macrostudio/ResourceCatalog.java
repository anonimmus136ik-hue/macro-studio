package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Inventory-only resource filter: never examines unknown or hidden world blocks.
 * Each category can contain raw materials, ingots and silk-touch ore equivalents.
 */
public final class ResourceCatalog {
    public static final String[] NAMES = {"DIAMOND", "EMERALD", "GOLD", "IRON", "REDSTONE", "COAL", "STONE"};
    private static final Item[][] GROUPS = {
        {Items.DIAMOND, Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE},
        {Items.EMERALD, Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE},
        {Items.GOLD_INGOT, Items.RAW_GOLD, Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE},
        {Items.IRON_INGOT, Items.RAW_IRON, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE},
        {Items.REDSTONE, Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE},
        {Items.COAL, Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE},
        {Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.STONE}
    };
    private ResourceCatalog() {}

    public static boolean matches(ItemStack stack, int type) {
        if (stack == null || stack.isEmpty() || type < 0 || type >= GROUPS.length) return false;
        for (Item item : GROUPS[type]) if (stack.is(item)) return true;
        return false;
    }

    public static int count(Minecraft mc, int type) {
        if (mc == null || mc.player == null || type < 0 || type >= GROUPS.length) return 0;
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (matches(stack, type)) count += stack.getCount();
        }
        return count;
    }

    public static boolean selected(int type) {
        boolean[] filter = Settings.I.resourceFilter;
        return filter != null && type >= 0 && type < filter.length && filter[type];
    }

    /** Guard market clipboard: only a checked resource, not tools or rare equipment. */
    public static boolean eligibleForMarket(ItemStack held) {
        if (held == null || held.isEmpty() || held.getCount() < 64
                || held.getMaxStackSize() < 64 || !held.getEnchantments().isEmpty()) return false;
        for (int type = 0; type < GROUPS.length; type++) {
            if (selected(type) && matches(held, type)) return true;
        }
        return false;
    }
}
