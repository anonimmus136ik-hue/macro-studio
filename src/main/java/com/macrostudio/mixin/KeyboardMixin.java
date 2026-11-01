package com.macrostudio.mixin;

import com.macrostudio.Keys;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
	@Inject(method = "keyPress", at = @At("HEAD"))
	private void macrostudio$key(long window, int action, KeyEvent event, CallbackInfo ci) {
		Keys.onRawKey(event.key(), action);
	}
}
