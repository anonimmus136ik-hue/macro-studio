package com.macrostudio.mixin;

import com.macrostudio.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class CritMixin {
	@Inject(method = "crit", at = @At("HEAD"), require = 0)
	private void macrostudio$crit(Entity target, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (Settings.I.critSound && (Object) this == mc.player) {
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.4F));
		}
	}
}
