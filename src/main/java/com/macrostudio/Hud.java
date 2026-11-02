package com.macrostudio;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Simple text overlay in the top right corner: coordinates, armor durability, waypoints. */
public final class Hud {
	private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
	private static final String[] ARMOR_NAMES = { "Шлем", "Нагрудник", "Штаны", "Ботинки" };

	private Hud() {
	}

	public static void init() {
		HudRenderCallback.EVENT.register((g, tickCounter) -> render(g));
	}

	private static String facing(float yaw) {
		float y = ((yaw % 360f) + 360f) % 360f;
		if (y >= 315f || y < 45f) return "Ю";
		if (y < 135f) return "З";
		if (y < 225f) return "С";
		return "В";
	}

	private static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		Settings s = Settings.I;
		if (mc.player == null || mc.options.hideGui) return;
        if (s.digHud) {
            String progress = Selection3D.hudText();
            if (!progress.isEmpty()) {
                int maxText = Math.max(12, Math.min(95, (mc.getWindow().getGuiScaledWidth() - 18) / 6));
                if (progress.length() > maxText) progress = progress.substring(0, maxText - 2) + "..";
                int w = mc.font.width(progress) + 16;
                g.fill(5, 5, 5 + w, 26, 0xD6111D2C);
                g.fill(5, 5, 8, 26, Selection3D.selecting() ? 0xFF4EEDAA : 0xFFFB7285);
                g.drawString(mc.font, progress, 13, 12, 0xFFFFFFFF);
            }
        }
		List<String> lines = new ArrayList<>();
		if (s.hudCoords) {
			lines.add(String.format("XYZ %d %d %d  %s", (int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()),
					(int) Math.floor(mc.player.getZ()), facing(mc.player.getYRot())));
		}
		if (s.hudArmor) {
			for (int i = 0; i < ARMOR.length; i++) {
				ItemStack st = mc.player.getItemBySlot(ARMOR[i]);
				if (!st.isEmpty() && st.isDamageableItem()) {
					int left = st.getMaxDamage() - st.getDamageValue();
					lines.add(ARMOR_NAMES[i] + ": " + left * 100 / Math.max(1, st.getMaxDamage()) + "%");
				}
			}
		}
		if (s.hudWaypoints) {
			for (Settings.Waypoint w : s.waypoints) {
				double dx = w.x + 0.5 - mc.player.getX();
				double dy = w.y - mc.player.getY();
				double dz = w.z + 0.5 - mc.player.getZ();
				lines.add(w.name + ": " + (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + " м");
			}
		}
		int sw = mc.getWindow().getGuiScaledWidth();
		int y = 4;
		for (String l : lines) {
			g.drawString(mc.font, l, sw - mc.font.width(l) - 4, y, 0xFFFFFFFF);
			y += 10;
		}
	}
}
