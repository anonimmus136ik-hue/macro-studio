package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Records one frame per client tick: view angles, selected slot, pressed keys and discrete events. */
public final class Recorder {
	public static boolean active = false;

	private static Macro current;
	private static List<Ev> pending = new ArrayList<>();
	private static boolean wasContainer = false;

	private Recorder() {
	}

	public static void msg(String text) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) mc.player.displayClientMessage(Component.literal("[Macro Studio] " + text), true);
	}

	public static void start() {
		if (active) return;
		if (Replayer.active) Replayer.stop();
		Keys.takeTapped();
		current = new Macro();
		current.name = Macros.nextAutoName();
		current.frames = new ArrayList<>();
		pending = new ArrayList<>();
		wasContainer = false;
		active = true;
		msg("Запись идёт...");
	}

	/** Stops and saves. Returns the saved name or null. */
	public static String stop() {
		if (!active) return null;
		active = false;
		String name = null;
		if (current != null && !current.frames.isEmpty()) {
			Macros.save(current);
			name = current.name;
			msg("Сохранено: " + name + " (" + current.frames.size() + " тиков)");
		}
		current = null;
		return name;
	}

	public static void tick(Minecraft mc) {
		if (!active) return;
		if (mc.player == null) {
			stop();
			return;
		}
		boolean container = mc.screen instanceof AbstractContainerScreen<?>;
		if (wasContainer && !container) pending.add(Ev.close());
		wasContainer = container;

		Set<Integer> down = new HashSet<>(Keys.pollDown());
		down.addAll(Keys.frameTapped);
		down.removeAll(Keys.suppressed);
		down.removeAll(Settings.reservedKeys());
		for (Settings.Bind b : Settings.I.binds) down.remove(b.key);
		down.removeAll(Keys.chatKeys(mc));
		if (mc.screen != null) down.clear();

		Frame f = new Frame();
		f.y = mc.player.getYRot();
		f.p = mc.player.getXRot();
		f.s = mc.player.getInventory().getSelectedSlot();
		f.k = new int[down.size()];
		int i = 0;
		for (int c : down) f.k[i++] = c;
		java.util.Arrays.sort(f.k);
		f.e = pending.isEmpty() ? null : pending;
		pending = new ArrayList<>();
		current.frames.add(f);
	}

	private static boolean ok() {
		return active && !Replayer.injecting && !Binds.sending;
	}

	/** Called when a key bind of this mod sends a line, so it is stored in the macro exactly once. */
	public static void addBind(String text) {
		if (!active || Replayer.injecting) return;
		pending.add(text.startsWith("/") ? Ev.cmd(text.substring(1)) : Ev.chat(text));
	}

	public static void onCommand(String cmd) {
		if (ok()) pending.add(Ev.cmd(cmd));
	}

	public static void onChat(String text) {
		if (ok()) pending.add(Ev.chat(text));
	}

	public static void onClick(int slot, int button, String type) {
		if (ok()) pending.add(Ev.click(slot, button, type));
	}
}
