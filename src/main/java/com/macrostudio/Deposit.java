package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * When the inventory is full while the digger works: run a command, walk to the chests,
 * put the items in, run the return command and restart the digger.
 */
public final class Deposit {
	private enum St { IDLE, CMD1, GO, OPEN, WAIT_SCREEN, MOVE, CMD2 }

	private static St st = St.IDLE;
	private static int ci;
	private static int t;
	private static int lastSlot = -1;
	private static int sameCount = 0;

	private Deposit() {
	}

	/** Slot index in the player's own menu for an inventory index (0-8 hotbar, 9-35 main). */
	private static int menuIdx(int inv) {
		return inv < 9 ? 36 + inv : inv;
	}

	/** Inventory index for the i-th slot of an opened container menu with n slots. */
	private static int invOfMenuSlot(int i, int n) {
		int k = i - (n - 36);
		return k < 27 ? 9 + k : k - 27;
	}

	/** Full = every slot that may be deposited holds an item (even a single item counts). */
	public static boolean full(Minecraft mc) {
		boolean any = false;
		for (int i = 0; i < 36; i++) {
			if (Settings.I.keepSlots[i]) continue;
			any = true;
			if (!mc.player.inventoryMenu.slots.get(menuIdx(i)).hasItem()) return false;
		}
		return any;
	}

	private static boolean depositHasItems(Minecraft mc) {
		for (int i = 0; i < 36; i++) {
			if (Settings.I.keepSlots[i]) continue;
			if (mc.player.inventoryMenu.slots.get(menuIdx(i)).hasItem()) return true;
		}
		return false;
	}

	public static void stop() {
		if (st == St.IDLE) return;
		st = St.IDLE;
		Digger.paused = false;
		Digger.releaseKeys(Minecraft.getInstance());
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.gameMode == null) {
			st = St.IDLE;
			return;
		}
		Settings s = Settings.I;
		switch (st) {
			case IDLE -> {
				if (s.autoDeposit && !s.chests.isEmpty() && Digger.active && !Digger.paused
						&& mc.screen == null && full(mc)) {
					Digger.paused = true;
					Digger.releaseKeys(mc);
					ci = 0;
					t = 0;
					if (!s.fullCmd.isBlank()) Binds.start(s.fullCmd);
					st = St.CMD1;
					Recorder.msg("Инвентарь полон: сдаю ресурсы");
				}
			}
			case CMD1 -> {
				if (!Binds.idle()) return;
				if (++t < 30) return;
				t = 0;
				st = St.GO;
			}
			case GO -> {
				if (ci >= s.chests.size()) {
					finishChests(mc);
					return;
				}
				int[] c = s.chests.get(ci);
				double dx = c[0] + 0.5 - mc.player.getX();
				double dz = c[2] + 0.5 - mc.player.getZ();
				double dy = c[1] + 0.5 - mc.player.getY();
				if (Math.sqrt(dx * dx + dz * dz) < 2.4 && Math.abs(dy) < 4) {
					Digger.releaseKeys(mc);
					t = 0;
					st = St.OPEN;
					return;
				}
				if (++t > 300) {
					Digger.releaseKeys(mc);
					nextChest(mc);
					return;
				}
				mc.player.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
				mc.player.setXRot(10f);
				mc.options.keyUp.setDown(true);
				mc.options.keyJump.setDown(mc.player.horizontalCollision);
			}
			case OPEN -> {
				int[] c = s.chests.get(ci);
				BlockPos p = new BlockPos(c[0], c[1], c[2]);
				Vec3 ctr = new Vec3(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
				Vec3 eye = mc.player.getEyePosition();
				double dx = ctr.x - eye.x;
				double dy = ctr.y - eye.y;
				double dz = ctr.z - eye.z;
				mc.player.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
				mc.player.setXRot((float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))));
				mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(ctr, Direction.UP, p, false));
				t = 0;
				lastSlot = -1;
				sameCount = 0;
				st = St.WAIT_SCREEN;
			}
			case WAIT_SCREEN -> {
				if (mc.screen instanceof AbstractContainerScreen<?> && !(mc.screen instanceof InventoryScreen)) {
					t = 0;
					st = St.MOVE;
				} else if (++t > 40) {
					nextChest(mc);
				}
			}
			case MOVE -> {
				if (!(mc.screen instanceof AbstractContainerScreen<?> scr)) {
					nextChest(mc);
					return;
				}
				List<Slot> slots = scr.getMenu().slots;
				int n = slots.size();
				int pick = -1;
				for (int i = Math.max(0, n - 36); i < n; i++) {
					if (Settings.I.keepSlots[invOfMenuSlot(i, n)]) continue;
					if (slots.get(i).hasItem()) {
						pick = i;
						break;
					}
				}
				if (pick < 0) {
					mc.player.closeContainer();
					nextChest(mc);
					return;
				}
				if (pick == lastSlot) sameCount++;
				else sameCount = 0;
				lastSlot = pick;
				if (sameCount >= 3) { // chest is full
					mc.player.closeContainer();
					nextChest(mc);
					return;
				}
				mc.gameMode.handleInventoryMouseClick(scr.getMenu().containerId, pick, 0, ClickType.QUICK_MOVE, mc.player);
			}
			case CMD2 -> {
				if (!Binds.idle()) return;
				if (++t < 30) return;
				st = St.IDLE;
				if (full(mc)) {
					Digger.stop();
					Recorder.msg("Сундуки заполнены, копатель остановлен");
					return;
				}
				Digger.paused = false;
				Digger.start(mc);
			}
		}
	}

	private static void nextChest(Minecraft mc) {
		ci++;
		t = 0;
		if (ci < Settings.I.chests.size() && depositHasItems(mc)) {
			st = St.GO;
		} else {
			finishChests(mc);
		}
	}

	private static void finishChests(Minecraft mc) {
		Digger.releaseKeys(mc);
		if (!Settings.I.backCmd.isBlank()) Binds.start(Settings.I.backCmd);
		t = 0;
		st = St.CMD2;
	}
}
