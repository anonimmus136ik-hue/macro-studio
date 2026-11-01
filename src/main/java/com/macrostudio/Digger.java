package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client-side assisted surface miner.
 *
 * It only breaks blocks which the player's actual view can hit. It never extends
 * reach, moves through obstacles, hides actions from the server, or spoofs
 * rotation packets. The player can pause or cancel at any time.
 */
public final class Digger {
    public static boolean active = false;
    /** Used by Deposit while it is running. */
    public static boolean paused = false;

    private static final int MAX_CELLS = 16384;
    private static final int MAX_TARGET_TICKS = 140;
    private static final double REACH = 4.3;
    private static final int MISSING = Integer.MIN_VALUE;

    private static final List<int[]> path = new ArrayList<>();
    private static final Map<Integer, Integer> topCache = new HashMap<>();
    private static final Set<BlockPos> skipped = new HashSet<>();
    private static int index = 0;
    private static int referenceY = 0;
    private static int depth = 1;
    private static BlockPos target = null;
    private static int targetTicks = 0;
    private static boolean manualPause = false;
    private static Vec3 lastWalkPosition;
    private static int noMovementTicks;
    private static String status = "Готов к запуску";
    private static String lastError = "";

    private Digger() {}

    public static String status() { return status; }
    public static String lastError() { return lastError; }
    public static boolean userPaused() { return manualPause; }
    public static boolean isPaused() { return manualPause || paused; }
    public static int totalCells() { return path.size(); }
    public static int completedCells() { return Math.min(index, path.size()); }
    public static int skippedBlocks() { return skipped.size(); }
    public static BlockPos currentTarget() { return target; }
    public static int progressPercent() { return path.isEmpty() ? 0 : (int) (100L * completedCells() / path.size()); }

    public static long selectedCells() {
        int[] a = Settings.I.dig1, b = Settings.I.dig2;
        if (!validPoint(a) || !validPoint(b)) return 0;
        return (Math.abs((long) a[0] - b[0]) + 1L) * (Math.abs((long) a[2] - b[2]) + 1L);
    }

    private static boolean validPoint(int[] point) {
        return point != null && point.length >= 3;
    }

    public static void clearSelection() {
        if (active) stop();
        Settings.I.dig1 = null;
        Settings.I.dig2 = null;
        path.clear();
        topCache.clear();
        skipped.clear();
        index = 0;
        target = null;
        status = "Область очищена";
        Settings.save();
    }

    public static void setLookedPoint(Minecraft mc, int which) {
        if (mc == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult hit)
                || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            Recorder.msg("Наведи прицел на блок, затем открой меню");
            return;
        }
        BlockPos pos = hit.getBlockPos();
        setCoords(which, pos);
    }

    /** Uses the exact block under the crosshair when the GUI was opened. */
    public static void setSavedLookedPoint(int which, int[] snapshot) {
        if (snapshot == null || snapshot.length < 3) {
            Recorder.msg("Наведи прицел на блок перед открытием меню");
            return;
        }
        setCoords(which, new BlockPos(snapshot[0], snapshot[1], snapshot[2]));
    }

    public static void setPoint(Minecraft mc, int which) {
        if (mc == null || mc.player == null) return;
        setCoords(which, mc.player.blockPosition());
    }

    private static void setCoords(int which, BlockPos pos) {
        if (active) stop();
        int[] point = {pos.getX(), pos.getY(), pos.getZ()};
        if (which == 1) Settings.I.dig1 = point;
        else Settings.I.dig2 = point;
        status = "Выбрана точка " + which;
        Settings.save();
        Recorder.msg("Точка " + which + ": " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }

    public static void start(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            fail("Сначала войди в мир");
            return;
        }
        if (!validPoint(Settings.I.dig1) || !validPoint(Settings.I.dig2)) {
            fail("Нужно выбрать обе точки (Pos1 / Pos2)");
            return;
        }
        long count = selectedCells();
        if (count <= 0 || count > MAX_CELLS) {
            fail("Площадь должна быть от 1 до " + MAX_CELLS + " колонок");
            return;
        }
        if (Replayer.active) Replayer.stop();
        if (active) stop();

        int[] a = Settings.I.dig1, b = Settings.I.dig2;
        int minX = Math.min(a[0], b[0]);
        int maxX = Math.max(a[0], b[0]);
        int minZ = Math.min(a[2], b[2]);
        int maxZ = Math.max(a[2], b[2]);
        referenceY = Math.max(a[1], b[1]);
        depth = Mth.clamp(Settings.I.digDepth, 1, 8);
        path.clear();
        int row = 0;
        for (int z = minZ; z <= maxZ; z++, row++) {
            if (row % 2 == 0) {
                for (int x = minX; x <= maxX; x++) path.add(new int[] {x, z});
            } else {
                for (int x = maxX; x >= minX; x--) path.add(new int[] {x, z});
            }
        }
        topCache.clear();
        skipped.clear();
        index = 0;
        target = null;
        targetTicks = 0;
        noMovementTicks = 0;
        lastWalkPosition = mc.player.position();
        paused = false;
        manualPause = false;
        active = true;
        lastError = "";
        status = "Запущено: " + path.size() + " колонок";
        Recorder.msg("Копатель запущен. STOP — в меню");
    }

    public static void togglePause() {
        if (!active) return;
        manualPause = !manualPause;
        if (manualPause) {
            status = "Пауза — нажми RESUME";
            releaseKeys(Minecraft.getInstance());
        } else {
            status = "Работа возобновлена";
        }
    }

    public static void stop() {
        if (!active) return;
        active = false;
        paused = false;
        manualPause = false;
        target = null;
        targetTicks = 0;
        noMovementTicks = 0;
        lastWalkPosition = null;
        releaseKeys(Minecraft.getInstance());
        status = "Остановлен пользователем";
        Recorder.msg("Копатель остановлен");
    }

    private static void finish() {
        active = false;
        paused = false;
        manualPause = false;
        target = null;
        releaseKeys(Minecraft.getInstance());
        status = "Готово. Обработано " + completedCells() + " / " + path.size();
        Recorder.msg("Копание завершено");
    }

    private static void fail(String reason) {
        lastError = reason;
        status = reason;
        Recorder.msg("Mining: " + reason);
    }

    private static void halt(String reason) {
        stop();
        fail(reason);
    }

    public static void releaseKeys(Minecraft mc) {
        if (mc == null || mc.options == null) return;
        mc.options.keyAttack.setDown(false);
        mc.options.keyUp.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keySprint.setDown(false);
    }

    private static boolean safeFloor(BlockState state) {
        return state.blocksMotion() && !state.is(BlockTags.LEAVES);
    }

    private static int surface(Minecraft mc, int i) {
        Integer cached = topCache.get(i);
        if (cached != null) return cached;
        int[] cell = path.get(i);
        int result = MISSING;
        for (int y = referenceY + 3; y >= referenceY - 8; y--) {
            BlockState state = mc.level.getBlockState(new BlockPos(cell[0], y, cell[1]));
            if (safeFloor(state)) {
                result = y;
                break;
            }
        }
        topCache.put(i, result);
        return result;
    }

    private static boolean underPlayer(Minecraft mc, int x, int z) {
        double px = mc.player.getX(), pz = mc.player.getZ();
        return x >= Math.floor(px - 0.45) && x <= Math.floor(px + 0.45)
                && z >= Math.floor(pz - 0.45) && z <= Math.floor(pz + 0.45);
    }

    private static BlockPos findTarget(Minecraft mc) {
        Vec3 eye = mc.player.getEyePosition();
        int from = Math.max(0, index - 10);
        int to = Math.min(index, path.size() - 1);
        for (int i = from; i <= to; i++) {
            int top = surface(mc, i);
            if (top == MISSING) continue;
            int[] col = path.get(i);
            if (underPlayer(mc, col[0], col[1])) continue;
            for (int y = top; y > top - depth; y--) {
                BlockPos pos = new BlockPos(col[0], y, col[1]);
                if (skipped.contains(pos) || mc.level.getBlockState(pos).isAir()) continue;
                Vec3 center = Vec3.atCenterOf(pos);
                if (eye.distanceTo(center) <= REACH && unobstructed(mc, pos)) return pos;
            }
        }
        return null;
    }

    private static boolean unobstructed(Minecraft mc, BlockPos pos) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        if (eye.distanceTo(center) > REACH) return false;
        BlockHitResult hit = mc.level.clip(new ClipContext(
                eye, center, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.BLOCK && pos.equals(hit.getBlockPos());
    }

    /** Checks the *actual camera ray*, not only the line to the target centre. */
    private static boolean actualAimHits(Minecraft mc, BlockPos pos) {
        Vec3 start = mc.player.getEyePosition();
        Vec3 end = start.add(Vec3.directionFromRotation(
                mc.player.getXRot(), mc.player.getYRot()).scale(REACH));
        BlockHitResult hit = mc.level.clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.BLOCK && pos.equals(hit.getBlockPos());
    }

    /** Smooth, deterministic camera movement. Does not spoof rotation packets. */
    private static boolean turn(Minecraft mc, float desiredYaw, float desiredPitch) {
        float max = Mth.clamp(Settings.I.digTurnSpeed, 2, 20);
        float yawDelta = Mth.wrapDegrees(desiredYaw - mc.player.getYRot());
        float pitchDelta = desiredPitch - mc.player.getXRot();
        // Deceleration near target to prevent oscillation.
        float yawStep = Mth.clamp(yawDelta * 0.48f, -max, max);
        float pitchStep = Mth.clamp(pitchDelta * 0.48f, -max, max);
        mc.player.setYRot(mc.player.getYRot() + yawStep);
        mc.player.setXRot(Mth.clamp(mc.player.getXRot() + pitchStep, -90f, 90f));
        return Math.abs(yawDelta) <= 2.0f && Math.abs(pitchDelta) <= 2.0f;
    }

    private static boolean aim(Minecraft mc, BlockPos pos) {
        Vec3 eye = mc.player.getEyePosition();
        double dx = pos.getX() + .5 - eye.x;
        double dy = pos.getY() + .5 - eye.y;
        double dz = pos.getZ() + .5 - eye.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        return turn(mc, yaw, pitch);
    }

    public static void tick(Minecraft mc) {
        if (!active) return;
        if (mc == null || mc.player == null || mc.level == null) {
            active = false;
            status = "Нет игрового мира";
            releaseKeys(mc);
            return;
        }
        if (path.isEmpty() || index >= path.size()) {
            finish();
            return;
        }
        if (manualPause || paused || mc.screen != null) {
            releaseKeys(mc);
            return;
        }
        if (mc.gameMode == null) {
            stop();
            return;
        }

        int top = surface(mc, index);
        if (top != MISSING && mc.player.getY() < top - 1.5) {
            halt("Падение относительно уровня земли — остановлено");
            return;
        }

        if (target != null && mc.level.getBlockState(target).isAir()) {
            target = null;
            targetTicks = 0;
        }
        if (target == null) target = findTarget(mc);
        if (target != null) {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keySprint.setDown(false);
            boolean aligned = aim(mc, target);
            boolean hit = aligned && unobstructed(mc, target) && actualAimHits(mc, target);
            mc.options.keyAttack.setDown(hit);
            status = hit ? "Копаю " + target.toShortString() : "Навожу камеру";
            if (++targetTicks > MAX_TARGET_TICKS) {
                mc.options.keyAttack.setDown(false);
                skipped.add(target);
                target = null;
                targetTicks = 0;
            }
            return;
        }
        mc.options.keyAttack.setDown(false);

        int[] cell = path.get(index);
        double dx = cell[0] + .5 - mc.player.getX();
        double dz = cell[1] + .5 - mc.player.getZ();
        double distance = Math.hypot(dx, dz);
        if (distance < 0.34) {
            releaseKeys(mc);
            int next = index + 1;
            int gap = 0;
            while (next < path.size() && surface(mc, next) == MISSING) {
                gap++;
                next++;
            }
            if (next >= path.size()) {
                finish();
                return;
            }
            if (gap >= 3) {
                halt("Большая пропасть впереди — остановлено");
                return;
            }
            index = next;
            status = "Перехожу к колонке " + (index + 1) + " из " + path.size();
            return;
        }

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        boolean facing = turn(mc, yaw, 8f);
        double radians = Math.toRadians(yaw);
        int xAhead = (int) Math.floor(mc.player.getX() - Math.sin(radians) * 0.85);
        int zAhead = (int) Math.floor(mc.player.getZ() + Math.cos(radians) * 0.85);
        int floorY = (int) Math.floor(mc.player.getY()) - 1;
        boolean dangerousGap = !safeFloor(mc.level.getBlockState(new BlockPos(xAhead, floorY, zAhead)))
                && !safeFloor(mc.level.getBlockState(new BlockPos(xAhead, floorY - 1, zAhead)));
        if (dangerousGap) {
            halt("Опасный провал впереди — остановлено");
            return;
        }
        if (lastWalkPosition != null && mc.player.position().distanceToSqr(lastWalkPosition) < 0.0025) {
            noMovementTicks++;
        } else {
            noMovementTicks = 0;
            lastWalkPosition = mc.player.position();
        }
        if (noMovementTicks > 120) {
            halt("Нет движения 6 секунд — путь заблокирован");
            return;
        }
        mc.options.keyUp.setDown(facing);
        mc.options.keyJump.setDown(facing && mc.player.horizontalCollision);
        mc.options.keySprint.setDown(false);
        status = "Двигаюсь к колонке " + (index + 1);
    }
}
