package com.macrostudio;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class ShulkerPreview {

    private static final int MAX_LINES = 15;
    private static boolean registered = false;

    private ShulkerPreview() {
    }

    public static void tick(Minecraft mc) {
        if (registered || mc == null) {
            return;
        }

        registered = true;

        ItemTooltipCallback.EVENT.register(
                (stack, context, type, lines) -> {

            if (!Settings.I.shulkerPreview) {
                return;
            }

            if (stack.isEmpty()) {
                return;
            }

            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                return;
            }

            if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
                return;
            }

            ItemContainerContents contents =
                    stack.get(DataComponents.CONTAINER);

            if (contents == null) {
                lines.add(Component.literal(
                        "Содержимое неизвестно")
                        .withStyle(ChatFormatting.DARK_GRAY));
                return;
            }

            int total = 0;
            int shown = 0;

            for (ItemStack item : contents.nonEmptyItems()) {
                if (item.isEmpty()) {
                    continue;
                }

                total++;

                if (shown >= MAX_LINES) {
                    continue;
                }

                String name = item.getHoverName().getString();
                int count = item.getCount();

                lines.add(Component.literal(
                        name + " x" + count)
                        .withStyle(ChatFormatting.GRAY));

                shown++;
            }

            if (total == 0) {
                lines.add(Component.literal("Пустой шалкер")
                        .withStyle(ChatFormatting.GRAY));
            } else if (total > shown) {
                lines.add(Component.literal(
                        "И ещё стопок: " + (total - shown))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        });
    }
}
