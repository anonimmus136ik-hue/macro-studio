package com.macrostudio;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * In-world selection preview. Key 0 starts selection; use right-click three times:
 * ground corner A (XZ + floor Y), ground corner B (XZ), final height Y.
 * Red means blocks to mine; green means a *candidate* exterior walkway,
 * checked against currently loaded block states. It is NOT pathfinding.
 *
 * No hidden-block ESP and no changes to block interaction outside selection.
 */
public final class Selection3D {
    private static boolean selecting;
    private static int phase;
    private static String hint = "";
    private static int messageTicks;

    private Selection3D() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(Selection3D::render);
    }

    public static boolean selecting() { return selecting; }
    public static int phase() { return phase; }
    public static String hint() { return hint; }

    public static void start() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        if (Digger.active) Digger.stop();
        selecting = true;
        phase = 1;
        inform("0: РЕЖИМ 3D | ПКМ: первая точка на земле (X/Z)");
    }

    public static void toggle() {
        if (selecting) {
            selecting = false;
            phase = 0;
            inform("3D выделение выключено. Область сохранена");
        } else start();
    }

    public static void cancel() {
        selecting = false;
        phase = 0;
        inform("Режим выделения отменён");
    }

    public static void reset() {
        selecting = false;
        phase = 0;
        Digger.clearSelection();
        Settings.I.digHeightPoint = null;
        Settings.save();
        inform("Выделение очищено");
    }

    public static void tick(Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) {
            selecting = false;
            phase = 0;
            return;
        }
        if (messageTicks > 0) messageTicks--;
    }

    public static boolean onRightClick(Minecraft mc) {
        if (!selecting || mc == null || mc.screen != null) return false;
        if (mc.player == null || mc.level == null) return false;
        if (!(mc.hitResult instanceof BlockHitResult hit)
                || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            inform("Наведись на блок. ПКМ выбирает следующую точку");
            return true; // Do not place/interact while selection is armed.
        }
        BlockPos p = hit.getBlockPos();
        Settings s = Settings.I;
        if (phase == 1) {
            Digger.clearSelection();
            s.dig1 = new int[] {p.getX(), p.getY(), p.getZ()};
            s.dig2 = null;
            s.digHeightPoint = null;
            s.digDimension = mc.level.dimension().identifier().toString();
            phase = 2;
            inform("Первая точка: " + coords(p) + ". ПКМ: противоположный угол на земле");
        } else if (phase == 2) {
            if (s.dig1 == null) { phase = 1; return true; }
            // Horizontal rectangle: both corners have exactly the same floor Y.
            s.dig2 = new int[] {p.getX(), s.dig1[1], p.getZ()};
            phase = 3;
            inform("Площадь: " + Digger.selectedCells() + " клеток. ПКМ: верхняя/нижняя высота Y");
        } else if (phase == 3) {
            s.digHeightPoint = new int[] {p.getX(), p.getY(), p.getZ()};
            selecting = false;
            phase = 0;
            long volume = Digger.selectedVolume();
            inform("3D готово: " + volume + " блоков. КРАСНЫЙ = копать, ЗЕЛЁНЫЙ = дорожка");
        }
        Settings.save();
        return true;
    }

    private static String coords(BlockPos p) {
        return p.getX() + " " + p.getY() + " " + p.getZ();
    }

    private static void inform(String message) {
        hint = message;
        messageTicks = 140;
        Recorder.msg(message);
    }

    public static String hudText() {
        if (selecting) return switch (phase) {
            case 1 -> "3D [0] ШАГ 1/3  |  ПКМ: первая точка X/Z";
            case 2 -> "3D [0] ШАГ 2/3  |  ПКМ: второй угол X/Z";
            case 3 -> "3D [0] ШАГ 3/3  |  ПКМ: высота Y";
            default -> "3D [0]";
        };
        if (Digger.active) return "MINING " + (Digger.isPaused() ? "PAUSED" : "RUNNING")
                + " | " + Digger.progressPercent() + "% | " + Digger.status();
        return messageTicks > 0 ? hint : "";
    }

    private static BlockPos aimed(Minecraft mc) {
        return mc.hitResult instanceof BlockHitResult h
                && mc.hitResult.getType() == HitResult.Type.BLOCK ? h.getBlockPos() : null;
    }

    private static boolean goodWalkCell(Minecraft mc, int x, int floorY, int z) {
        if (mc.level == null) return false;
        BlockPos floor = new BlockPos(x, floorY, z);
        BlockState block = mc.level.getBlockState(floor);
        return block.blocksMotion()
                && mc.level.getBlockState(floor.above()).isAir()
                && mc.level.getBlockState(floor.above(2)).isAir();
    }

    private static void render(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!selecting && !Settings.I.digOverlay) return;
        if (ctx.consumers() == null || ctx.matrices() == null) return;

        Settings s = Settings.I;
        int[] a = s.dig1;
        int[] b = s.dig2;
        BlockPos hover = selecting ? aimed(mc) : null;
        if (selecting && phase == 1 && hover != null) {
            a = new int[] {hover.getX(), hover.getY(), hover.getZ()};
            b = a;
        }
        if (a == null || a.length < 3) return;
        if (b == null && selecting && phase == 2 && hover != null)
            b = new int[] {hover.getX(), a[1], hover.getZ()};
        if (b == null || b.length < 3) b = a;

        int minX = Math.min(a[0], b[0]), maxX = Math.max(a[0], b[0]);
        int minZ = Math.min(a[2], b[2]), maxZ = Math.max(a[2], b[2]);
        int finalY = s.digHeightPoint != null && s.digHeightPoint.length >= 3
                ? s.digHeightPoint[1] : a[1];
        if (selecting && phase == 3 && hover != null) finalY = hover.getY();
        int lowY = Math.min(a[1], finalY), highY = Math.max(a[1], finalY);
        long width = (long) maxX - minX + 1, length = (long) maxZ - minZ + 1;
        if (width > 128 || length > 128 || (highY - lowY) > 48) return;

        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        Matrix4f pose = ctx.matrices().last().pose();
        VertexConsumer v = ctx.consumers().getBuffer(RenderTypes.lines());

        // RED: a true, 3-dimensional, depth-tested wireframe of the mining volume.
        box(v, pose, camera, minX, lowY, minZ, maxX + 1, highY + 1, maxZ + 1,
                1.0f, 0.23f, 0.32f, 1.0f);

        // Red grid over the TOP surface, with a render cap for large selections.
        int plotted = 0;
        int step = (int) Math.max(1, Math.ceil(Math.sqrt(width * length / 260.0)));
        for (int x = minX; x <= maxX && plotted < 300; x += step)
            for (int z = minZ; z <= maxZ && plotted < 300; z += step) {
                if (camera.distanceToSqr(new Vec3(x + .5, highY + 1, z + .5)) > 48 * 48) continue;
                box(v, pose, camera, x + .04, highY + 1.015, z + .04,
                        Math.min(x + step, maxX + 1) - .04,
                        highY + 1.035,
                        Math.min(z + step, maxZ + 1) - .04,
                        1f, .28f, .34f, .65f);
                plotted++;
            }

        // GREEN: one-block-wide walkable ground OUTSIDE the red region.
        // Never color a gap or obstructed two-block-high corridor green.
        if (s.digSafeWalkPreview) {
            int drawn = 0;
            int floorY = a[1];
            for (int x = minX - 1; x <= maxX + 1 && drawn < 260; x++) {
                for (int z = minZ - 1; z <= maxZ + 1 && drawn < 260; z++) {
                    if (x != minX - 1 && x != maxX + 1 && z != minZ - 1 && z != maxZ + 1) continue;
                    if (camera.distanceToSqr(new Vec3(x + .5, floorY + 1, z + .5)) > 40 * 40) continue;
                    if (!goodWalkCell(mc, x, floorY, z)) continue;
                    box(v, pose, camera, x + .09, floorY + 1.025, z + .09,
                            x + .91, floorY + 1.045, z + .91,
                            .18f, 1.0f, .48f, 1.0f);
                    drawn++;
                }
            }
        }
    }

    private static void box(VertexConsumer v, Matrix4f m, Vec3 camera,
                            double x1, double y1, double z1,
                            double x2, double y2, double z2,
                            float r, float g, float b, float a) {
        line(v,m,camera,x1,y1,z1,x2,y1,z1,r,g,b,a);
        line(v,m,camera,x2,y1,z1,x2,y1,z2,r,g,b,a);
        line(v,m,camera,x2,y1,z2,x1,y1,z2,r,g,b,a);
        line(v,m,camera,x1,y1,z2,x1,y1,z1,r,g,b,a);
        line(v,m,camera,x1,y2,z1,x2,y2,z1,r,g,b,a);
        line(v,m,camera,x2,y2,z1,x2,y2,z2,r,g,b,a);
        line(v,m,camera,x2,y2,z2,x1,y2,z2,r,g,b,a);
        line(v,m,camera,x1,y2,z2,x1,y2,z1,r,g,b,a);
        line(v,m,camera,x1,y1,z1,x1,y2,z1,r,g,b,a);
        line(v,m,camera,x2,y1,z1,x2,y2,z1,r,g,b,a);
        line(v,m,camera,x2,y1,z2,x2,y2,z2,r,g,b,a);
        line(v,m,camera,x1,y1,z2,x1,y2,z2,r,g,b,a);
    }

    private static void line(VertexConsumer v, Matrix4f pose, Vec3 camera,
                             double x1,double y1,double z1,double x2,double y2,double z2,
                             float r,float g,float b,float a) {
        float dx=(float)(x2-x1), dy=(float)(y2-y1), dz=(float)(z2-z1);
        float d=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(d<0.00001f) return;
        float nx=dx/d, ny=dy/d, nz=dz/d;
        v.addVertex((float)(x1-camera.x),(float)(y1-camera.y),(float)(z1-camera.z))
                .setColor(r,g,b,a).setNormal(nx,ny,nz).setLineWidth(1.7f);
        v.addVertex((float)(x2-camera.x),(float)(y2-camera.y),(float)(z2-camera.z))
                .setColor(r,g,b,a).setNormal(nx,ny,nz).setLineWidth(1.7f);
    }
}
