package com.macrostudio.mixin;

import com.macrostudio.Keys;
import com.macrostudio.Selection3D;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
	@Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
	private void macrostudio$mouse(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		Keys.onRawMouse(info.button(), action);
        if (info.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS
                && Selection3D.onRightClick(Minecraft.getInstance())) {
            ci.cancel(); // Selection consumes right-click, preventing unwanted placement.
        }
	}
}
