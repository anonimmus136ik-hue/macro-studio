package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.Set;

/** Small client-side quality-of-life features: weather, time, brightness, zoom, sprint, low health alarm. */
public final class Extras {
	public static final String[] TIME_NAMES = { "Выкл", "Утро", "День", "Закат", "Ночь" };
	private static final long[] TIMES = { 0L, 1000L, 6000L, 12500L, 18000L };

	private static boolean zooming = false;
	private static int savedFov = 70;
	private static int hpCooldown = 0;

	private Extras() {
	}

	public static void tick(Minecraft mc, Set<Integer> down) {
		Settings s = Settings.I;
		if (mc.level != null) {
			if (s.noRain) {
				mc.level.setRainLevel(0f);
				mc.level.setThunderLevel(0f);
			}
			if (s.timeMode > 0 && s.timeMode < TIMES.length) {
				mc.level.setTimeFromServer(mc.level.getGameTime(), TIMES[s.timeMode], false);
			}
		}
		if (s.brightMax && mc.options.gamma().get() < 1.0) {
			mc.options.gamma().set(1.0);
		}

		boolean z = s.zoomKey >= 0 && down.contains(s.zoomKey) && mc.screen == null;
		if (z && !zooming) {
			savedFov = mc.options.fov().get();
			zooming = true;
		}
		if (z) {
			mc.options.fov().set(30);
		} else if (zooming) {
			mc.options.fov().set(savedFov);
			zooming = false;
		}

		if (mc.player == null) return;
		if (s.autoSprint && mc.screen == null && mc.options.keyUp.isDown()) {
			mc.options.keySprint.setDown(true);
		}
		if (hpCooldown > 0) hpCooldown--;
		if (s.hpAlarm && hpCooldown == 0 && mc.player.getHealth() > 0 && mc.player.getHealth() <= s.hpHearts * 2f) {
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5F));
			hpCooldown = 20;
		}
	}
}
