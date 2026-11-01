package com.macrostudio.mixin;

import com.macrostudio.Keys;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
	@Inject(method = "onButton", at = @At("HEAD"))
	private void macrostudio$mouse(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		Keys.onRawMouse(info.button(), action);
	}
}
