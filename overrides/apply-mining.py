#!/usr/bin/env python3
"""Apply reviewed mining UI/turning improvements to the packaged Macro Studio source tree.
The GitHub repository currently stores its Gradle project inside a ZIP, so this
script overlays that original source during CI without changing unrelated files.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'work')
base = root / 'src/main/java/com/macrostudio'


def edit(name, fn):
    path = base / name
    old = path.read_text('utf-8')
    new = fn(old)
    if new == old:
        raise RuntimeError(f'No edits made to {path}; check the project version')
    path.write_text(new, 'utf-8')
    print(f'Updated {name}')


def once(s, old, new):
    if s.count(old) != 1:
        raise RuntimeError(f'Expected exactly one match, got {s.count(old)} for: {old[:90]!r}')
    return s.replace(old, new, 1)


def settings(s):
    return once(s, 'public int digDepth = 2;',
                'public int digDepth = 2;\n\t/** Maximum camera turn in degrees per client tick. */\n\tpublic int digTurnSpeed = 6;')


def digger(s):
    s = once(s, 'import net.minecraft.core.BlockPos;',
             'import net.minecraft.core.BlockPos;\nimport net.minecraft.util.Mth;\nimport net.minecraft.world.level.ClipContext;\nimport net.minecraft.world.phys.BlockHitResult;\nimport net.minecraft.world.phys.HitResult;')
    s = once(s, '\tprivate Digger() {\n\t}', '''\tprivate Digger() {\n\t}

\tpublic static int totalCells() { return path.size(); }
\tpublic static int completedCells() { return Math.min(idx, path.size()); }
\tpublic static int skippedBlocks() { return skip.size(); }
\tpublic static BlockPos currentTarget() { return target; }

\t/** Capture a corner from the last block the player was looking at. */
\tpublic static void setLookedPoint(Minecraft mc, int which) {
\t\tif (mc.player == null || !(mc.hitResult instanceof BlockHitResult hit)
\t\t\t\t|| mc.hitResult.getType() != HitResult.Type.BLOCK) {
\t\t\tRecorder.msg("Посмотри на блок перед открытием меню");
\t\t\treturn;
\t\t}
\t\tBlockPos p = hit.getBlockPos();
\t\tint[] v = {p.getX(), p.getY(), p.getZ()};
\t\tif (which == 1) Settings.I.dig1 = v;
\t\telse Settings.I.dig2 = v;
\t\tSettings.save();
\t\tRecorder.msg("Точка " + which + ": " + p.getX() + " " + p.getY() + " " + p.getZ());
\t}''')
    s = once(s, '''\t\tint minX = Math.min(s.dig1[0], s.dig2[0]);''', '''\t\tlong count = (Math.abs((long)s.dig1[0] - s.dig2[0]) + 1)
\t\t\t\t* (Math.abs((long)s.dig1[2] - s.dig2[2]) + 1);
\t\tif (count > 16384) {
\t\t\tRecorder.msg("Максимум 16384 клетки за один запуск");
\t\t\treturn;
\t\t}
\t\tint minX = Math.min(s.dig1[0], s.dig2[0]);''')
    s = once(s, '''\t\t\taim(mc, target);
\t\t\tmc.options.keyAttack.setDown(true);''', '''\t\t\tboolean aligned = aim(mc, target);
\t\t\t// Never attack until the visible ray actually reaches the chosen block.
\t\t\tmc.options.keyAttack.setDown(aligned && canSee(mc, target));''')
    s = once(s, '''\t\tfloat yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
\t\tmc.player.setYRot(yaw);
\t\tmc.player.setXRot(15f);
\t\tmc.options.keyUp.setDown(true);''', '''\t\tfloat yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
\t\tboolean facing = turn(mc, yaw, 15f);
\t\t// Wait until the camera has caught up before walking forward.
\t\tmc.options.keyUp.setDown(facing);''')
    s = once(s, '''\t\tmc.options.keySprint.setDown(chasm);
\t\tmc.options.keyJump.setDown(chasm || mc.player.horizontalCollision);''', '''\t\t// Conservative navigation: do not sprint/jump blindly over deep gaps.
\t\tmc.options.keySprint.setDown(false);
\t\tif (chasm) {
\t\t\tmc.options.keyUp.setDown(false);
\t\t\tmc.options.keyJump.setDown(false);
\t\t\tstop();
\t\t\tRecorder.msg("Копатель: обнаружена опасная яма");
\t\t\treturn;
\t\t}
\t\tmc.options.keyJump.setDown(facing && mc.player.horizontalCollision);''')
    s = once(s, '''\t\t\t\tif (ctr.distanceTo(eye) <= REACH) return p;''', '''\t\t\t\tif (ctr.distanceTo(eye) <= REACH && canSee(mc, p)) return p;''')
    old = '''\tprivate static void aim(Minecraft mc, BlockPos p) {
\t\tVec3 eye = mc.player.getEyePosition();
\t\tdouble dx = p.getX() + 0.5 - eye.x;
\t\tdouble dy = p.getY() + 0.5 - eye.y;
\t\tdouble dz = p.getZ() + 0.5 - eye.z;
\t\tmc.player.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
\t\tmc.player.setXRot((float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))));
\t}'''
    new = '''\t/** Smooth view movement. This does not send spoofed rotation packets. */
\tprivate static boolean aim(Minecraft mc, BlockPos p) {
\t\tVec3 eye = mc.player.getEyePosition();
\t\tdouble dx = p.getX() + 0.5 - eye.x;
\t\tdouble dy = p.getY() + 0.5 - eye.y;
\t\tdouble dz = p.getZ() + 0.5 - eye.z;
\t\tfloat yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
\t\tfloat pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
\t\treturn turn(mc, yaw, pitch);
\t}

\tprivate static boolean turn(Minecraft mc, float targetYaw, float targetPitch) {
\t\tfloat max = Mth.clamp(Settings.I.digTurnSpeed, 2, 20);
\t\tfloat yawDelta = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
\t\tfloat pitchDelta = targetPitch - mc.player.getXRot();
\t\tmc.player.setYRot(mc.player.getYRot() + Mth.clamp(yawDelta, -max, max));
\t\tmc.player.setXRot(Mth.clamp(mc.player.getXRot()
\t\t\t\t+ Mth.clamp(pitchDelta, -max, max), -90.0f, 90.0f));
\t\treturn Math.abs(yawDelta) <= 3f && Math.abs(pitchDelta) <= 3f;
\t}

\tprivate static boolean canSee(Minecraft mc, BlockPos p) {
\t\tVec3 eye = mc.player.getEyePosition();
\t\tVec3 center = new Vec3(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
\t\tif (eye.distanceTo(center) > REACH) return false;
\t\tBlockHitResult hit = mc.level.clip(new ClipContext(eye, center,
\t\t\t\tClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
\t\treturn hit.getType() == HitResult.Type.BLOCK && p.equals(hit.getBlockPos());
\t}'''
    s = once(s, old, new)
    return s


def screen(s):
    s = once(s, 'private static final int ACCENT = 0xFF4FC3F7;', 'private static final int ACCENT = 0xFF50D6BE;')
    s = once(s, 'private static final int TEXT = 0xFFE6E8EE;', 'private static final int TEXT = 0xFFF1F6FC;')
    s = once(s, 'private static final int MUTED = 0xFF8A90A0;', 'private static final int MUTED = 0xFFA0AEC2;')
    s = once(s, 'pw = Math.min(width - 16, 440);', 'pw = Math.min(width - 20, 610);')
    s = once(s, 'ph = height - 16;', 'ph = Math.min(height - 20, 435);')
    s = once(s, 'py = 8;', 'py = (height - ph) / 2;')
    start = s.index('\tprivate void buildDig() {')
    end = s.index('\n\tprivate String onOff', start)
    s = s[:start] + '''\tprivate void buildDig() {
\t\tMinecraft mc = Minecraft.getInstance();
\t\tint x = px + 15;
\t\tint y = py + 62;
\t\tint w = Math.min(pw - 30, 270);
\t\tint half = (w - 6) / 2;
\t\tbtn("Т1: блок", x, y + 28, half, 21, b -> { Digger.setLookedPoint(mc, 1); rebuild(); });
\t\tbtn("Т1: стою", x + half + 6, y + 28, half, 21, b -> { Digger.setPoint(mc, 1); rebuild(); });
\t\tbtn("Т2: блок", x, y + 91, half, 21, b -> { Digger.setLookedPoint(mc, 2); rebuild(); });
\t\tbtn("Т2: стою", x + half + 6, y + 91, half, 21, b -> { Digger.setPoint(mc, 2); rebuild(); });
\t\tbtn("−", x, y + 157, 36, 21, b -> {
\t\t\tSettings.I.digDepth = Math.max(1, Settings.I.digDepth - 1);
\t\t\tSettings.save(); rebuild();
\t\t});
\t\tbtn("+", x + w - 36, y + 157, 36, 21, b -> {
\t\t\tSettings.I.digDepth = Math.min(8, Settings.I.digDepth + 1);
\t\t\tSettings.save(); rebuild();
\t\t});
\t\tbtn("−", x, y + 217, 36, 21, b -> {
\t\t\tSettings.I.digTurnSpeed = Math.max(2, Settings.I.digTurnSpeed - 1);
\t\t\tSettings.save(); rebuild();
\t\t});
\t\tbtn("+", x + w - 36, y + 217, 36, 21, b -> {
\t\t\tSettings.I.digTurnSpeed = Math.min(20, Settings.I.digTurnSpeed + 1);
\t\t\tSettings.save(); rebuild();
\t\t});
\t\tbtn(Digger.active ? "ОСТАНОВИТЬ" : "ЗАПУСТИТЬ КОПАТЕЛЬ", x, y + 260, w, 27, b -> {
\t\t\tif (Digger.active) { Digger.stop(); rebuild(); }
\t\t\telse { Digger.start(mc); if (Digger.active) onClose(); else rebuild(); }
\t\t});
\t}
''' + s[end:]
    s = once(s, 'g.fill(0, 0, width, height, 0xB0000000);', 'g.fill(0, 0, width, height, 0xCD070E19);')
    s = once(s, 'g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF2B3140);', 'g.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xFF354A62);')
    s = once(s, 'g.fill(px, py, px + pw, py + ph, 0xF012151C);', 'g.fill(px, py, px + pw, py + ph, 0xF00D1522);')
    s = once(s, 'g.fill(px, py, px + pw, py + 21, 0xFF1B2030);', 'g.fill(px, py, px + pw, py + 21, 0xFF1D2B40);')
    s = once(s, 'if (mode == Mode.MACROS) {', '''if (mode == Mode.DIG) {
\t\t\tint lx = px + 10, ty = py + 56;
\t\t\tg.fill(lx, ty, lx + 280, ty + Math.min(ph - 68, 328), 0xFF142437);
\t\t\tg.fill(lx, ty, lx + 280, ty + 2, ACCENT);
\t\t\tif (pw >= 560) {
\t\t\t\tint rx = px + 305, rw = pw - 318;
\t\t\t\tg.fill(rx, ty, rx + rw, ty + Math.min(ph - 68, 328), 0xFF142437);
\t\t\t\tg.fill(rx, ty, rx + rw, ty + 2, 0xFF8C78E7);
\t\t\t\tdrawDigGrid(g, rx + 14, ty + 42, rw - 28, 165);
\t\t\t}
\t\t}
\t\tif (mode == Mode.MACROS) {''')
    s = once(s, 'g.drawString(font, "Macro Studio", px + 8, py + 7, ACCENT);', 'g.drawString(font, "MACRO STUDIO 2.0", px + 8, py + 7, ACCENT);')
    start = s.index('\t\t\tcase DIG -> {', s.index('public void render(GuiGraphics'))
    end = s.index('\n\t\t\tcase DEPOSIT', start)
    s = s[:start] + '''\t\t\tcase DIG -> {
\t\t\t\tint x = px + 16, y = py + 62;
\t\t\t\tg.drawString(font, "MINING / AREA CONTROL", x, y + 3, ACCENT);
\t\t\t\tg.drawString(font, "ТОЧКА 1", x, y + 16, TEXT);
\t\t\t\tg.drawString(font, pointLabel(Settings.I.dig1), x, y + 54, MUTED);
\t\t\t\tg.drawString(font, "ТОЧКА 2", x, y + 79, TEXT);
\t\t\t\tg.drawString(font, pointLabel(Settings.I.dig2), x, y + 118, MUTED);
\t\t\t\tg.drawString(font, "ГЛУБИНА", x, y + 144, TEXT);
\t\t\t\tg.drawCenteredString(font, Settings.I.digDepth + " слоя", x + 135, y + 163, 0xFFFFD28A);
\t\t\t\tg.drawString(font, "ПОВОРОТ КАМЕРЫ", x, y + 202, TEXT);
\t\t\t\tg.drawCenteredString(font, Settings.I.digTurnSpeed + " °/тик", x + 135, y + 223, 0xFFFFD28A);
\t\t\t\tif (pw >= 560) {
\t\t\t\t\tint rx = px + 305;
\t\t\t\t\tg.drawString(font, "PREVIEW / TOP VIEW", rx + 14, y + 3, 0xFF9A8BFF);
\t\t\t\t\tg.drawString(font, "Колонок: " + digArea(), rx + 14, y + 222, TEXT);
\t\t\t\t\tg.drawString(font, Digger.active ? "КОПАНИЕ" : "ОЖИДАНИЕ", rx + 14, y + 240, MUTED);
\t\t\t\t\tint total = Digger.totalCells();
\t\t\t\t\tint p = total == 0 ? 0 : Digger.completedCells() * 100 / total;
\t\t\t\t\tint rw = pw - 346;
\t\t\t\t\tg.fill(rx + 14, y + 264, rx + 14 + rw, y + 272, 0xFF38495D);
\t\t\t\t\tg.fill(rx + 14, y + 264, rx + 14 + rw * p / 100, y + 272, ACCENT);
\t\t\t\t\t}
\t\t\t\tg.drawString(font, "Копание только видимых блоков. Esc — закрыть меню.", x, hintY, MUTED);
\t\t\t}
''' + s[end:]
    # Add helper functions before the keyPressed override; avoids changing existing widgets.
    needle = '\t// ---------------- input ----------------'
    helper = '''\tprivate static String pointLabel(int[] p) {
\t\treturn p == null || p.length < 3 ? "Не выбрана" : p[0] + " / " + p[1] + " / " + p[2];
\t}

\tprivate static long digArea() {
\t\tint[] a = Settings.I.dig1, b = Settings.I.dig2;
\t\tif (a == null || b == null || a.length < 3 || b.length < 3) return 0;
\t\treturn (Math.abs((long) a[0] - b[0]) + 1) * (Math.abs((long) a[2] - b[2]) + 1);
\t}

\tprivate void drawDigGrid(GuiGraphics g, int x, int y, int w, int h) {
\t\tg.fill(x, y, x + w, y + h, 0xFF0B1725);
\t\tint[] a = Settings.I.dig1, b = Settings.I.dig2;
\t\tif (a == null || b == null || a.length < 3 || b.length < 3) {
\t\t\tg.drawCenteredString(font, "Выбери две точки", x + w / 2, y + h / 2, MUTED);
\t\t\treturn;
\t\t}
\t\tint cols = (int) Math.min(24, Math.abs((long) a[0] - b[0]) + 1);
\t\tint rows = (int) Math.min(16, Math.abs((long) a[2] - b[2]) + 1);
\t\tint cell = Math.max(2, Math.min((w - 8) / cols, (h - 8) / rows));
\t\tint ox = x + (w - cols * cell) / 2, oy = y + (h - rows * cell) / 2;
\t\tg.fill(ox - 2, oy - 2, ox + cols * cell + 2, oy + rows * cell + 2, ACCENT);
\t\tfor (int rz = 0; rz < rows; rz++) {
\t\t\tfor (int cx = 0; cx < cols; cx++) {
\t\t\t\tint px0 = ox + cx * cell, py0 = oy + rz * cell;
\t\t\t\tg.fill(px0 + 1, py0 + 1, px0 + cell - 1, py0 + cell - 1,
\t\t\t\t\t\t(rz % 2 == 0) ? 0xFF25576A : 0xFF20495B);
\t\t\t}
\t\t}
\t}

'''
    s = once(s, needle, helper + needle)
    return s

edit('Settings.java', settings)
edit('Digger.java', digger)
edit('MacroStudioScreen.java', screen)