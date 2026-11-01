package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class AutoTool {

    private static final int MIN_DURABILITY = 10;

    private static boolean active = false;
    private static int originalSlot = -1;
    private static int automaticSlot = -1;

    private AutoTool() {
    }

    public static void tick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            clear();
            return;
        }

        Inventory inventory = mc.player.getInventory();

        boolean mining = Settings.I.autoTool
                && mc.screen == null
                && mc.options.keyAttack.isDown()
                && mc.hitResult instanceof BlockHitResult;

        if (!mining) {
            restore(inventory);
            return;
        }

        BlockHitResult hit = (BlockHitResult) mc.hitResult;
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);

        if (state.isAir()) {
            restore(inventory);
            return;
        }

        int selected = inventory.getSelectedSlot();

        if (active && selected != automaticSlot) {
            // The player or another module changed the slot.
            // Do not override a manual choice.
            clear();
        }

        int bestSlot = selected;
        float bestSpeed = -1.0F;
        boolean bestCorrect = false;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            if (!hasEnoughDurability(stack)) {
                continue;
            }

            float speed = stack.getDestroySpeed(state);
            boolean correct = stack.isCorrectToolForDrops(state);

            // Correct harvesting takes priority over speed.
            if (correct && !bestCorrect) {
                bestCorrect = true;
                bestSpeed = speed;
                bestSlot = slot;
            } else if (correct == bestCorrect
                    && speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }

        if (bestSlot == selected) {
            return;
        }

        // Avoid switching to an equally slow item
        // when no harvesting advantage exists.
        if (!bestCorrect && bestSpeed <= 1.0F) {
            return;
        }

        if (!active) {
            originalSlot = selected;
            active = true;
        }

        inventory.setSelectedSlot(bestSlot);
        automaticSlot = bestSlot;
    }

    private static boolean hasEnoughDurability(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return true;
        }

        int remaining = stack.getMaxDamage()
                - stack.getDamageValue();

        return remaining > MIN_DURABILITY;
    }

    private static void restore(Inventory inventory) {
        if (active
                && originalSlot >= 0
                && originalSlot < 9
                && inventory.getSelectedSlot() == automaticSlot) {
            inventory.setSelectedSlot(originalSlot);
        }

        clear();
    }

    private static void clear() {
        active = false;
        originalSlot = -1;
        automaticSlot = -1;
    }
}
