package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-world selection outlines for Minecraft 1.21.11, using native Gizmos.
 * No chunks or hidden blocks are scanned. The drill is an estimated footprint,
 * not a claim about what a particular server-side custom item will break.
 */
public final class SelectionVisualizer {
    private static final int EMERALD = 0xFF42E8B8;
    private static final int GOLD = 0xFFE6BF80;
    private static final int AMBER = 0xFFF5A44D;
    private static final int BLUE = 0xFF73A9FF;
    private static final int RED = 0xFFF07583;

    private static long lastRouteTime = -1;
    private static java.util.List<BlockPos> routeCache = java.util.List.of();

    private SelectionVisualizer() {}

    public static void tick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null || mc.options.hideGui) return;
        Settings s = Settings.I;
        if (!s.selectionVisuals && !s.drillPreview && !s.showRoute && !s.buildPreview) return;

        try (Gizmos.TemporaryCollection ignored = mc.collectPerTickGizmos()) {
            if (s.selectionVisuals) selection(mc, s);
            if (s.drillPreview) drill(mc);
            if (s.showRoute) route(mc, s);
            if (s.buildPreview) build(mc, s);
        }
    }

    private static void selection(Minecraft mc, Settings s) {
        int[] a = s.dig1, b = s.dig2;
        if (a == null || b == null || a.length < 3 || b.length < 3) return;

        int minX = Math.min(a[0], b[0]), maxX = Math.max(a[0], b[0]);
        int minZ = Math.min(a[2], b[2]), maxZ = Math.max(a[2], b[2]);
        int originY = s.digUp ? Math.min(a[1], b[1]) : Math.max(a[1], b[1]);
        int d = Math.max(1, Math.min(32, s.digDepth));
        int minY = s.digUp ? originY : originY - d + 1;
        int maxY = s.digUp ? originY + d - 1 : originY;

        // Keep large remote selections from consuming render time.
        double midX = (minX + maxX + 1) * 0.5, midZ = (minZ + maxZ + 1) * 0.5;
        if (mc.player.distanceToSqr(midX, mc.player.getY(), midZ) > 128.0 * 128.0) return;

        Gizmos.cuboid(new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1),
                GizmoStyle.strokeAndFill(EMERALD, 2.5f, 0x1342E8B8));

        if (s.showActiveLayer) {
            int y = Digger.active && Digger.currentTarget() != null
                    ? Digger.currentTarget().getY() : originY;
            y = Math.max(minY, Math.min(maxY, y));
            Gizmos.cuboid(new AABB(minX, y, minZ, maxX + 1, y + 1, maxZ + 1),
                    GizmoStyle.strokeAndFill(BLUE, 2f, 0x1973A9FF));
        }

        Gizmos.cuboid(new BlockPos(a[0], a[1], a[2]), GizmoStyle.stroke(GOLD, 3f));
        Gizmos.cuboid(new BlockPos(b[0], b[1], b[2]), GizmoStyle.stroke(AMBER, 3f));
        if (Digger.currentTarget() != null && Digger.active) {
            Gizmos.cuboid(Digger.currentTarget(), GizmoStyle.stroke(RED, 3f));
        }
    }


    private static void route(Minecraft mc, Settings s) {
        if (s.dig2 == null || s.dig2.length < 3) return;
        long gameTime = mc.level.getGameTime();
        if (lastRouteTime < 0 || gameTime < lastRouteTime || gameTime - lastRouteTime >= 20) {
            lastRouteTime = gameTime;
            BlockPos dest = Digger.currentTarget() == null
                    ? new BlockPos(s.dig2[0], s.dig2[1], s.dig2[2]) : Digger.currentTarget();
            routeCache = SmartPath.toMiningColumn(mc, dest.getX(), dest.getZ()).path();
        }
        for (int i = 1; i < routeCache.size() && i < 96; i++) {
            BlockPos prev = routeCache.get(i - 1), next = routeCache.get(i);
            Gizmos.line(Vec3.atCenterOf(prev).add(0, -0.30, 0),
                    Vec3.atCenterOf(next).add(0, -0.30, 0), BLUE, 3f);
        }
    }

    private static void build(Minecraft mc, Settings s) {
        if (s.dig1 == null || s.dig2 == null) return;
        int[] a = s.dig1, b = s.dig2;
        int minX = Math.min(a[0], b[0]), maxX = Math.max(a[0], b[0]) + 1;
        int minZ = Math.min(a[2], b[2]), maxZ = Math.max(a[2], b[2]) + 1;
        int minY = Math.min(a[1], b[1]), maxY = Math.max(a[1], b[1]) + 1;
        if (mc.player.distanceToSqr((minX + maxX) / 2.0, mc.player.getY(),
                (minZ + maxZ) / 2.0) > 128 * 128) return;
        GizmoStyle style = GizmoStyle.strokeAndFill(BLUE, 2f, 0x1873A9FF);
        switch (Math.floorMod(s.buildMode, 3)) {
            case 0 -> Gizmos.cuboid(new AABB(minX, minY, minZ, maxX, minY + 1, maxZ), style);
            case 1 -> {
                Gizmos.cuboid(new AABB(minX, minY, minZ, minX + 1, maxY, maxZ), style);
                Gizmos.cuboid(new AABB(maxX - 1, minY, minZ, maxX, maxY, maxZ), style);
                if (maxX - minX > 2) {
                    Gizmos.cuboid(new AABB(minX + 1, minY, minZ, maxX - 1, maxY, minZ + 1), style);
                    Gizmos.cuboid(new AABB(minX + 1, minY, maxZ - 1, maxX - 1, maxY, maxZ), style);
                }
            }
            default -> Gizmos.cuboid(new AABB(minX, minY, minZ, maxX, maxY, maxZ), style);
        }
    }

    private static void drill(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult hit) || mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        Direction facing = hit.getDirection();
        BlockPos origin = hit.getBlockPos();
        // Build a 3x3 cross-section orthogonal to the face we are looking at,
        // and one extra layer behind it (3 x 3 x 2).
        Direction axis1, axis2;
        if (facing.getAxis() == Direction.Axis.Y) {
            axis1 = Direction.EAST;
            axis2 = Direction.SOUTH;
        } else if (facing.getAxis() == Direction.Axis.X) {
            axis1 = Direction.UP;
            axis2 = Direction.SOUTH;
        } else {
            axis1 = Direction.UP;
            axis2 = Direction.EAST;
        }
        for (int layer = 0; layer < 2; layer++) {
            BlockPos middle = origin.relative(facing.getOpposite(), layer);
            for (int u = -1; u <= 1; u++) for (int v = -1; v <= 1; v++) {
                BlockPos block = middle.relative(axis1, u).relative(axis2, v);
                if (mc.player.distanceToSqr(Vec3.atCenterOf(block)) > 9 * 9) continue;
                Gizmos.cuboid(block, GizmoStyle.stroke(layer == 0 ? AMBER : GOLD, layer == 0 ? 2f : 1f));
            }
        }
    }
}
