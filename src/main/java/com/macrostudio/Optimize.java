package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Decides which other players are not drawn at all (saves FPS at crowded spawn). */
public final class Optimize {
	private static int players = 0;

	private Optimize() {
	}

	public static void tick(Minecraft mc) {
		players = mc.level == null ? 0 : mc.level.players().size();
	}

	public static boolean hide(Entity e) {
		Settings s = Settings.I;
		if (!s.optOn) return false;
		Minecraft mc = Minecraft.getInstance();
		if (!(e instanceof Player) || e == mc.player || mc.player == null) return false;
		double d2 = e.distanceToSqr(mc.player);
		if (s.optCrowd > 0 && players > s.optCrowd && d2 > 36.0) return true;
		if (s.optDist > 0 && d2 > (double) s.optDist * s.optDist) return true;
		if (s.optZone && s.optZ1 != null && s.optZ2 != null) {
			int minX = Math.min(s.optZ1[0], s.optZ2[0]);
			int maxX = Math.max(s.optZ1[0], s.optZ2[0]) + 1;
			int minZ = Math.min(s.optZ1[2], s.optZ2[2]);
			int maxZ = Math.max(s.optZ1[2], s.optZ2[2]) + 1;
			double x = e.getX();
			double z = e.getZ();
			if (x >= minX && x < maxX && z >= minZ && z < maxZ) return true;
		}
		return false;
	}

	public static void setZonePoint(Minecraft mc, int which) {
		if (mc.player == null) return;
		net.minecraft.core.BlockPos p = mc.player.blockPosition();
		int[] v = new int[] { p.getX(), p.getY(), p.getZ() };
		if (which == 1) Settings.I.optZ1 = v;
		else Settings.I.optZ2 = v;
		Settings.save();
	}
}
