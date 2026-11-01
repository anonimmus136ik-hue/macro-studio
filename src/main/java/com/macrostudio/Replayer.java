package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;

import java.util.HashSet;
import java.util.Set;

/** Plays a recorded macro back, one frame per client tick. */
public final class Replayer {
	public static boolean active = false;
	/** True while the replayer itself performs a click, so it is not recorded again. */
	public static boolean injecting = false;

	private static Macro macro;
	private static boolean loop;
	private static int index;
	private static int evPos;
	private static boolean applied;
	private static int waited;
	private static Set<Integer> held = new HashSet<>();

	private Replayer() {
	}

	public static String currentName() {
		return active && macro != null ? macro.name : null;
	}

	public static void play(Macro m, boolean looped) {
		if (m == null || m.frames == null || m.frames.isEmpty()) return;
		if (Recorder.active) Recorder.stop();
		macro = m;
		loop = looped;
		reset();
		active = true;
		Recorder.msg("Воспроизведение: " + m.name + (looped ? " (цикл)" : ""));
	}

	private static void reset() {
		index = 0;
		evPos = 0;
		applied = false;
		waited = 0;
	}

	public static void stop() {
		if (!active) return;
		active = false;
		releaseAll();
		Recorder.msg("Воспроизведение остановлено");
	}

	private static void releaseAll() {
		for (int c : held) Keys.release(c);
		held = new HashSet<>();
	}

	public static void tick(Minecraft mc) {
		if (!active) return;
		if (mc.player == null) {
			active = false;
			held = new HashSet<>();
			return;
		}
		if (index >= macro.frames.size()) {
			if (loop) {
				reset();
				releaseAll();
			} else {
				stop();
				return;
			}
		}
		Frame f = macro.frames.get(index);
		if (!applied) {
			mc.player.setYRot(f.y);
			mc.player.setXRot(f.p);
			if (f.s >= 0 && f.s <= 8) mc.player.getInventory().setSelectedSlot(f.s);
			applyKeys(f.k == null ? new int[0] : f.k);
			applied = true;
		}
		if (f.e != null) {
			while (evPos < f.e.size()) {
				if (!runEvent(mc, f.e.get(evPos))) return; // blocked, try again next tick
				evPos++;
				waited = 0;
			}
		}
		index++;
		evPos = 0;
		applied = false;
	}

	private static void applyKeys(int[] keys) {
		Set<Integer> now = new HashSet<>();
		for (int c : keys) now.add(c);
		for (int c : held) {
			if (!now.contains(c)) Keys.release(c);
		}
		for (int c : now) {
			if (held.contains(c)) Keys.hold(c);
			else Keys.press(c);
		}
		held = now;
	}

	/** Returns false when the event has to wait (for example, the chest has not opened yet). */
	private static boolean runEvent(Minecraft mc, Ev e) {
		switch (e.type) {
			case Ev.CMD -> {
				Binds.sending = true;
				try {
					mc.player.connection.sendCommand(e.text);
				} finally {
					Binds.sending = false;
				}
			}
			case Ev.CHAT -> {
				Binds.sending = true;
				try {
					mc.player.connection.sendChat(e.text);
				} finally {
					Binds.sending = false;
				}
			}
			case Ev.CLICK -> {
				if (!(mc.screen instanceof AbstractContainerScreen<?>)) {
					return ++waited > 100; // wait up to 5 seconds, then skip
				}
				ClickType t;
				try {
					t = ClickType.valueOf(e.click);
				} catch (Exception ex) {
					t = ClickType.PICKUP;
				}
				injecting = true;
				try {
					mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, e.slot, e.button, t, mc.player);
				} finally {
					injecting = false;
				}
			}
			case Ev.CLOSE -> {
				if (mc.screen instanceof AbstractContainerScreen<?>) mc.player.closeContainer();
			}
			default -> {
			}
		}
		return true;
	}
}
