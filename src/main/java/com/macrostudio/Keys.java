package com.macrostudio;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/**
 * Keyboard and mouse state helpers.
 * Codes below 1000 are GLFW key codes, codes from 1000 up are mouse buttons (1000 + button).
 */
public final class Keys {
	public static final int MOUSE_BASE = 1000;

	private static final int[] POLL = buildPollList();
	private static final Set<Integer> TAPPED = new HashSet<>();
	/** Keys pressed during the current tick, filled once per tick by the mod. */
	public static Set<Integer> frameTapped = new HashSet<>();

	private Keys() {
	}

	private static int[] buildPollList() {
		java.util.List<Integer> l = new java.util.ArrayList<>();
		l.add(32);
		l.add(39);
		for (int i = 44; i <= 57; i++) l.add(i);
		l.add(59);
		l.add(61);
		for (int i = 65; i <= 93; i++) l.add(i);
		l.add(96);
		l.add(161);
		l.add(162);
		for (int i = 256; i <= 269; i++) l.add(i);
		for (int i = 280; i <= 284; i++) l.add(i);
		for (int i = 290; i <= 314; i++) l.add(i);
		for (int i = 320; i <= 336; i++) l.add(i);
		for (int i = 340; i <= 348; i++) l.add(i);
		int[] a = new int[l.size()];
		for (int i = 0; i < a.length; i++) a[i] = l.get(i);
		return a;
	}

	// ---- raw events (called from mixins) ----

	/** Keys that were pressed while a screen was open; they are ignored until released. */
	public static final Set<Integer> suppressed = new HashSet<>();

	public static void onRawKey(int key, int action) {
		raw(key, action);
	}

	public static void onRawMouse(int button, int action) {
		raw(MOUSE_BASE + button, action);
	}

	private static void raw(int code, int action) {
		if (action == GLFW.GLFW_RELEASE) {
			suppressed.remove(code);
			return;
		}
		if (action != GLFW.GLFW_PRESS) return;
		if (Minecraft.getInstance().screen != null) {
			suppressed.add(code);
			return;
		}
		TAPPED.add(code);
	}

	/** Keys pressed since the previous call (even if already released again). */
	public static Set<Integer> takeTapped() {
		Set<Integer> s = new HashSet<>(TAPPED);
		TAPPED.clear();
		return s;
	}

	// ---- polling ----

	public static Set<Integer> pollDown() {
		Set<Integer> s = new HashSet<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.getWindow() == null) return s;
		long h = mc.getWindow().handle();
		for (int code : POLL) {
			if (GLFW.glfwGetKey(h, code) == GLFW.GLFW_PRESS) s.add(code);
		}
		for (int b = 0; b < 8; b++) {
			if (GLFW.glfwGetMouseButton(h, b) == GLFW.GLFW_PRESS) s.add(MOUSE_BASE + b);
		}
		return s;
	}

	// ---- game key mapping ----

	public static InputConstants.Key toKey(int code) {
		if (code >= MOUSE_BASE) return InputConstants.Type.MOUSE.getOrCreate(code - MOUSE_BASE);
		return InputConstants.Type.KEYSYM.getOrCreate(code);
	}

	public static void press(int code) {
		InputConstants.Key k = toKey(code);
		KeyMapping.set(k, true);
		KeyMapping.click(k);
	}

	public static void hold(int code) {
		KeyMapping.set(toKey(code), true);
	}

	public static void release(int code) {
		KeyMapping.set(toKey(code), false);
	}

	/** Codes of the chat / command keys, so typing them is not recorded as a key press. */
	public static Set<Integer> chatKeys(Minecraft mc) {
		Set<Integer> s = new HashSet<>();
		try {
			InputConstants.Key a = KeyBindingHelper.getBoundKeyOf(mc.options.keyChat);
			InputConstants.Key b = KeyBindingHelper.getBoundKeyOf(mc.options.keyCommand);
			if (a.getType() == InputConstants.Type.KEYSYM) s.add(a.getValue());
			if (b.getType() == InputConstants.Type.KEYSYM) s.add(b.getValue());
		} catch (Exception ignored) {
		}
		return s;
	}

	public static String name(int code) {
		if (code < 0) return "-";
		try {
			return toKey(code).getDisplayName().getString();
		} catch (Exception e) {
			return "#" + code;
		}
	}
}
