package com.macrostudio;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MacroStudioMod implements ClientModInitializer {
	private static Set<Integer> prevDown = new HashSet<>();
	private static boolean worldAnnounced = false;

	@Override
	public void onInitializeClient() {
		Settings.load();
		ClientSendMessageEvents.COMMAND.register(Recorder::onCommand);
		ClientSendMessageEvents.CHAT.register(Recorder::onChat);
		ClientTickEvents.START_CLIENT_TICK.register(MacroStudioMod::tick);
		Hud.init();
	}

	private static void tick(Minecraft mc) {
        if (mc.player == null) {
            worldAnnounced = false;
        } else if (!worldAnnounced) {
            worldAnnounced = true;
            Recorder.msg("Macro Studio MAX 2.4.0 Obsidian Edition загружен — Right Ctrl открывает меню");
        }
		Set<Integer> down = Keys.pollDown();
		Set<Integer> pressed = new HashSet<>(down);
		pressed.addAll(Keys.takeTapped());
		Keys.frameTapped = new HashSet<>(pressed);
		pressed.removeAll(prevDown);
		prevDown = down;

		if (mc.player != null && mc.screen == null) {
			for (int code : pressed) {
				if (!Keys.suppressed.contains(code)) handlePress(mc, code);
			}
		}

		AutoTool.tick(mc);
		AutoRefill.tick(mc);
		AutoEat.tick(mc);
		ShulkerPreview.tick(mc);
		Digger.tick(mc);
        SelectionVisualizer.tick(mc);
		Deposit.tick(mc);
		Extras.tick(mc, down);
		Optimize.tick(mc);
		Replayer.tick(mc);
		Binds.tick(mc);
		Recorder.tick(mc);
	}

	private static void handlePress(Minecraft mc, int code) {
		Settings s = Settings.I;
		if (code == s.overlayKey) {
            s.overlayEnabled = !s.overlayEnabled;
            Settings.save();
            Recorder.msg(s.overlayEnabled
                    ? "3D selection ON - green walking / red demolition"
                    : "3D selection OFF");
            return;
        }
        if (code == s.openKey) {
			mc.setScreen(new MacroStudioScreen());
			return;
		}
		if (code == s.recordKey) {
			if (Recorder.active) {
				Recorder.stop();
				s.recordArmed = false;
				Settings.save();
			} else {
                Recorder.start();
            }
			return;
		}
		if (code == s.stopKey) {
			if (Recorder.active) Recorder.stop();
			if (Replayer.active) Replayer.stop();
			Deposit.stop();
			if (Digger.active) Digger.stop();
			Binds.clear();
			return;
		}
		for (Map.Entry<String, Integer> en : s.macroKeys.entrySet()) {
			if (en.getValue() == code) {
				if (Replayer.active && en.getKey().equals(Replayer.currentName())) {
					Replayer.stop();
				} else {
					Replayer.play(Macros.load(en.getKey()), true);
				}
				return;
			}
		}
		if (!Replayer.active) Binds.trigger(code);
	}
}
