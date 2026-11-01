package com.macrostudio.mixin;

import com.macrostudio.Recorder;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class ClickMixin {
	@Inject(method = "handleInventoryMouseClick", at = @At("HEAD"))
	private void macrostudio$click(int containerId, int slotId, int buttonNum, ClickType clickType, Player player, CallbackInfo ci) {
		Recorder.onClick(slotId, buttonNum, clickType.name());
	}
}
