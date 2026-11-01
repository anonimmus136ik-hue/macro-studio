package com.macrostudio;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Key to command bindings that live inside this mod.
 * A bind line looks like: /warp mine ;; wait 40 ;; /home
 * "wait 20" waits 20 ticks, "wait 2s" waits 2 seconds. Lines without a slash are sent as chat.
 */
public final class Binds {
	/** True while the mod itself sends a message, so the recorder does not store it. */
	public static boolean sending = false;

	private static final List<Runner> RUNNERS = new ArrayList<>();

	private static final class Runner {
		final List<String> steps;
		int i = 0;
		int wait = 0;

		Runner(List<String> steps) {
			this.steps = steps;
		}
	}

	private Binds() {
	}

	/** Runs every bind that is assigned to this key code. */
	public static void trigger(int code) {
		for (Settings.Bind b : Settings.I.binds) {
			if (b.key == code && b.lines != null && !b.lines.isBlank()) {
				start(b.lines);
			}
		}
	}

	public static void start(String lines) {
		List<String> steps = new ArrayList<>();
		for (String part : lines.split(";;")) {
			String t = part.trim();
			if (!t.isEmpty()) steps.add(t);
		}
		if (!steps.isEmpty()) RUNNERS.add(new Runner(steps));
	}

	public static boolean idle() {
		return RUNNERS.isEmpty();
	}

	public static void clear() {
		RUNNERS.clear();
	}

	public static void tick(Minecraft mc) {
		if (RUNNERS.isEmpty()) return;
		if (mc.player == null) {
			RUNNERS.clear();
			return;
		}
		Iterator<Runner> it = RUNNERS.iterator();
		while (it.hasNext()) {
			Runner r = it.next();
			if (r.wait > 0) {
				r.wait--;
				continue;
			}
			while (r.i < r.steps.size()) {
				String step = r.steps.get(r.i++);
				if (step.toLowerCase().startsWith("wait")) {
					r.wait = parseWait(step.substring(4).trim());
					break;
				}
				send(mc, step);
			}
			if (r.i >= r.steps.size() && r.wait <= 0) it.remove();
		}
	}

	private static void send(Minecraft mc, String text) {
		Recorder.addBind(text);
		sending = true;
		try {
			if (text.startsWith("/")) {
				mc.player.connection.sendCommand(text.substring(1));
			} else {
				mc.player.connection.sendChat(text);
			}
		} finally {
			sending = false;
		}
	}

	private static int parseWait(String s) {
		try {
			String v = s.toLowerCase().trim();
			if (v.endsWith("s")) {
				return Math.max(0, Math.round(Float.parseFloat(v.substring(0, v.length() - 1).trim()) * 20f));
			}
			return Math.max(0, Integer.parseInt(v));
		} catch (Exception e) {
			return 20;
		}
	}
}
