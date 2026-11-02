package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Optional tool selector for custom mining tools.
 * The user registers the held drill and one block type that works with it.
 * All mining remains vanilla reach / interaction and is server-authoritative.
 * This does not emulate, multiply, or spoof the server's area-mining ability.
 */
public final class DrillAssist {
    private DrillAssist() {}

    public static void captureHeld(Minecraft mc) {
        if (mc == null || mc.player == null || mc.player.getMainHandItem().isEmpty()) {
            Recorder.msg("Hold your drill, then press SAVE HELD AS DRILL");
            return;
        }
        Settings.I.drillItemId = BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).toString();
        Settings.I.drillItemName = mc.player.getMainHandItem().getHoverName().getString();
        Settings.save();
        Recorder.msg("Drill item saved: " + Settings.I.drillItemId);
    }

    public static void captureBlock(Minecraft mc, int[] looked) {
        if (mc == null || mc.level == null || looked == null || looked.length < 3) {
            Recorder.msg("Look at a block before opening the GUI");
            return;
        }
        BlockState state = mc.level.getBlockState(new BlockPos(looked[0], looked[1], looked[2]));
        if (state.isAir()) {
            Recorder.msg("Select a solid block for this drill");
            return;
        }
        Settings.I.drillBlockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        Settings.save();
        Recorder.msg("Drill material saved: " + Settings.I.drillBlockId);
    }

    public static void select(Minecraft mc, BlockPos target) {
        if (mc == null || mc.player == null || mc.level == null || target == null
                || !Settings.I.drillAssist) return;
        BlockState state = mc.level.getBlockState(target);
        if (state.isAir()) return;
        Inventory inventory = mc.player.getInventory();
        String targetBlock = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        // Use the registered drill only for the explicitly compatible block.
        if (!Settings.I.drillItemId.isBlank() && !Settings.I.drillBlockId.isBlank()
                && Settings.I.drillBlockId.equals(targetBlock)) {
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty()
                        && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(Settings.I.drillItemId)
                        && (Settings.I.drillItemName.isBlank() || Settings.I.drillItemName.equals(stack.getHoverName().getString()))
                        && durable(stack)) {
                    inventory.setSelectedSlot(slot);
                    return;
                }
            }
        }

        // Fallback on an ordinary tool for mismatched/remaining blocks.
        int selected = inventory.getSelectedSlot();
        int bestSlot = -1;
        float bestSpeed = Float.NEGATIVE_INFINITY;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !durable(stack)) continue;
            if (!Settings.I.drillItemId.isBlank()
                    && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(Settings.I.drillItemId)
                    && (Settings.I.drillItemName.isBlank() || Settings.I.drillItemName.equals(stack.getHoverName().getString()))) continue;
            if (!stack.isCorrectToolForDrops(state)) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSlot = slot;
                bestSpeed = speed;
            }
        }
        if (bestSlot >= 0 && bestSlot != selected) inventory.setSelectedSlot(bestSlot);
    }

    private static boolean durable(ItemStack stack) {
        return !stack.isDamageableItem()
                || stack.getMaxDamage() - stack.getDamageValue() > 10;
    }
}
