package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public final class AutoRefill {

    private static final ItemStack[] remembered =
            new ItemStack[9];

    private static Player lastPlayer = null;
    private static int cooldown = 0;

    private AutoRefill() {
    }

    public static void tick(Minecraft mc) {
        if (mc == null || mc.player == null) {
            reset();
            return;
        }

        if (lastPlayer != mc.player) {
            reset();
            lastPlayer = mc.player;
        }

        if (!Settings.I.autoRefill) {
            observe(mc.player.getInventory());
            return;
        }

        Inventory inventory = mc.player.getInventory();

        if (mc.gameMode == null
                || mc.screen != null
                || mc.player.containerMenu != mc.player.inventoryMenu
                || !mc.player.inventoryMenu.getCarried().isEmpty()) {
            observe(inventory);
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            observeNonEmpty(inventory);
            return;
        }

        for (int slot = 0; slot < 9; slot++) {
            ItemStack current = inventory.getItem(slot);

            if (!current.isEmpty()) {
                remembered[slot] = current.copy();
                continue;
            }

            ItemStack previous = remembered[slot];

            if (previous == null || previous.isEmpty()) {
                continue;
            }

            int source = findReplacement(inventory, previous);

            if (source == -1) {
                // Remember the item for a later refill.
                continue;
            }

            // Inventory indices 9..35 map to menu slots 9..35.
            // SWAP button is the destination hotbar index 0..8.
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.inventoryMenu.containerId,
                    source,
                    slot,
                    ClickType.SWAP,
                    mc.player
            );

            cooldown = 5;
            return; // Only one inventory operation per cycle.
        }
    }

    private static int findReplacement(
            Inventory inventory,
            ItemStack wanted) {

        for (int slot = 9; slot < 36; slot++) {
            ItemStack candidate = inventory.getItem(slot);

            if (candidate.isEmpty()) {
                continue;
            }

            if (ItemStack.isSameItemSameComponents(
                    wanted, candidate)) {
                return slot;
            }
        }

        return -1;
    }

    private static void observe(Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            remembered[i] = inventory.getItem(i).copy();
        }
    }

    private static void observeNonEmpty(Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);

            if (!stack.isEmpty()) {
                remembered[i] = stack.copy();
            }
        }
    }

    private static void reset() {
        lastPlayer = null;
        cooldown = 0;

        for (int i = 0; i < remembered.length; i++) {
            remembered[i] = ItemStack.EMPTY;
        }
    }
}
