package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class AutoEat {

    private static boolean eating = false;
    private static boolean sawUsing = false;

    private static int originalSlot = -1;
    private static int foodSlot = -1;
    private static int startingCount = 0;
    private static int startingHunger = 0;
    private static int elapsed = 0;
    private static int cooldown = 0;

    private static Player lastPlayer = null;

    private AutoEat() {
    }

    public static void tick(Minecraft mc) {
        if (mc == null || mc.player == null) {
            stop(mc);
            lastPlayer = null;
            return;
        }

        if (lastPlayer != mc.player) {
            stop(mc);
            lastPlayer = mc.player;
        }

        if (!Settings.I.autoEat
                || Digger.active
                || mc.gameMode == null
                || mc.screen != null
                || mc.options.keyAttack.isDown()) {
            stop(mc);
            return;
        }

        Inventory inventory = mc.player.getInventory();

        if (eating) {
            if (inventory.getSelectedSlot() != foodSlot) {
                // Another module or the player changed the slot.
                stop(mc);
                return;
            }

            elapsed++;

            ItemStack current = inventory.getItem(foodSlot);
            int hunger = mc.player.getFoodData().getFoodLevel();

            if (hunger > startingHunger
                    || current.getCount() < startingCount
                    || current.isEmpty()) {
                stop(mc);
                cooldown = 10;
                return;
            }

            if (mc.player.isUsingItem()) {
                sawUsing = true;
            } else if (sawUsing) {
                stop(mc);
                cooldown = 10;
                return;
            }

            if (elapsed > 100
                    || (!sawUsing && elapsed > 12)) {
                stop(mc);
                cooldown = 40;
                return;
            }

            mc.options.keyUse.setDown(true);
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        int hunger = mc.player.getFoodData().getFoodLevel();
        int threshold = Math.max(1,
                Math.min(20, Settings.I.eatBelow));

        if (hunger >= threshold || hunger >= 20) {
            return;
        }

        // Do not interrupt the player's own item usage.
        if (mc.options.keyUse.isDown()
                || mc.player.isUsingItem()) {
            return;
        }

        int found = findFood(inventory);

        if (found == -1) {
            return;
        }

        originalSlot = inventory.getSelectedSlot();
        foodSlot = found;
        startingHunger = hunger;
        startingCount = inventory.getItem(found).getCount();

        elapsed = 0;
        sawUsing = false;
        eating = true;

        inventory.setSelectedSlot(foodSlot);

        // Start using the food item once.
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);

        // Keep the use key pressed while eating.
        mc.options.keyUse.setDown(true);
    }

    private static int findFood(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.get(DataComponents.FOOD) != null
                    && stack.get(DataComponents.CONSUMABLE) != null) {
                return slot;
            }
        }

        return -1;
    }

    private static void stop(Minecraft mc) {
        if (mc != null && eating) {
            mc.options.keyUse.setDown(false);

            if (mc.player != null) {
                Inventory inventory = mc.player.getInventory();

                if (originalSlot >= 0
                        && originalSlot < 9
                        && inventory.getSelectedSlot() == foodSlot) {
                    inventory.setSelectedSlot(originalSlot);
                }
            }
        }

        eating = false;
        sawUsing = false;
        originalSlot = -1;
        foodSlot = -1;
        startingCount = 0;
        startingHunger = 0;
        elapsed = 0;
    }

    public static boolean isEating() {
        return eating;
    }
}
