package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Macro Studio MAX UI. All actionable buttons correspond to existing code.
 * Decorative buttons are backed by vanilla Button widgets for keyboard and
 * mouse handling. The visible skin is custom-rendered.
 */
public final class MacroStudioScreen extends Screen {
    private enum Tab { HOME, MINING, ROUTE, BUILD, MARKET, AUTO, MACROS, BINDS, DEPOSIT, VISUAL, PERF, SETTINGS }
    private enum Tone { NORMAL, PRIMARY, DANGER, MUTED, SELECTED }

    private static final int BG = 0xFF090E15;
    private static final int PANEL = 0xFF121C27;
    private static final int CARD = 0xFF182533;
    private static final int BORDER = 0xFF2A4550;
    private static final int WHITE = 0xFFEAF5FF;
    private static final int GREY = 0xFF93A6BE;
    private static final int TEAL = 0xFF69E8BF;
    private static final int PURPLE = 0xFF9A8AFF;
    private static final int RED = 0xFFFF7184;
    private static final int GOLD = 0xFFD8BA80;
    private static final int BLUE = 0xFF80AFFF;

    private static final String[] NAV = {"DASHBOARD", "MINING STUDIO", "SMART ROUTE", "BUILD STUDIO", "MARKET / ORES", "AUTOMATION", "MACROS", "BINDS", "DEPOSIT", "VISUAL", "PERFORMANCE", "SETTINGS"};
    private final List<UiButton> paintedButtons = new ArrayList<>();
    private Tab tab = Tab.MINING;
    private String selectedMacro;
    private String rename = "";
    private int macroPage = 0;
    private int bindsPage = 0;
    private int navPage = 0;
    private int visualPage = 0;
    private String captureId;
    private Consumer<Integer> capture;
    private int px, py, pw, ph, side, cx, cy, cw, ch, rowStep;
    private int[] lookedPos;

    private record UiButton(Button button, String caption, Tone tone) {}

    public MacroStudioScreen() {
        super(Component.literal("Macro Studio | Obsidian Edition"));
        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult instanceof BlockHitResult hit && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            lookedPos = new int[] {hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ()};
        }
    }

    @Override
    protected void init() {
        build();
    }

    @Override
    public void removed() {
        Settings.save();
    }

    private static void rounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.min(7, Math.min(w, h) / 3);
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + w, y + h - r, color);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }

    private void drawCard(GuiGraphics g, int x, int y, int w, int h) {
        rounded(g, x, y, w, h, BORDER);
        rounded(g, x + 1, y + 1, w - 2, h - 2, CARD);
    }

    private void line(GuiGraphics g, String text, int x, int y, int color) {
        // DrawString clips no text. Shorten only at the rendering layer.
        g.drawString(font, text, x, y, color);
    }

    private Button button(String caption, int x, int y, int w, int h, Tone tone, Button.OnPress action) {
        Button b = Button.builder(Component.literal(caption), action).bounds(x, y, w, h).build();
        // Native button still receives mouse, keyboard and narration input.
        // Custom paint is drawn after the standard widget render.
        b.setAlpha(0.0f);
        addRenderableWidget(b);
        paintedButtons.add(new UiButton(b, caption, tone));
        return b;
    }

    private void callAndRefresh(Runnable r) {
        r.run();
        Settings.save();
        build();
    }

    private void build() {
        clearWidgets();
        paintedButtons.clear();
        pw = Math.max(190, Math.min(width - 12, Math.min(1100, width * Settings.I.uiWidthPercent / 100)));
        ph = Math.max(170, Math.min(height - 10, Math.min(680, height * Settings.I.uiHeightPercent / 100)));
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        side = Math.min(139, Math.max(102, pw / 5));
        cx = px + side + 13;
        cy = py + 62;
        cw = px + pw - 11 - cx;
        ch = ph - 74;
        rowStep = Math.max(14, Math.min(30, (ph - 101) / NAV.length));

        int navRows = Math.min(NAV.length, Math.max(3, (ph - 120) / 22));
        navPage = Math.max(0, Math.min(navPage, (NAV.length - 1) / navRows));
        int navStart = navPage * navRows;
        int tabHeight = Math.min(27, Math.max(16, (ph - 124) / navRows - 2));
        int tabStep = tabHeight + 3;
        for (int local = 0; local < navRows && navStart + local < NAV.length; local++) {
            int i = navStart + local;
            final Tab t = Tab.values()[i];
            button(NAV[i], px + 8, py + 67 + local * tabStep, side - 16,
                    tabHeight, tab == t ? Tone.SELECTED : Tone.MUTED,
                    b -> { tab = t; build(); });
        }
        if (NAV.length > navRows) {
            button("<", px + 8, py + ph - 52, Math.max(28, (side-22)/2), 23, Tone.MUTED,
                    b -> { navPage = Math.max(0, navPage-1); build(); });
            button(">", px + 11 + Math.max(28, (side-22)/2), py + ph - 52,
                    Math.max(28, (side-22)/2), 23, Tone.MUTED,
                    b -> { navPage = Math.min((NAV.length-1)/navRows,navPage+1); build(); });
        }
        button("X", px + pw - 34, py + 13, 22, 22, Tone.DANGER, b -> onClose());
        switch (tab) {
            case HOME -> home();
            case MINING -> mining();
            case ROUTE -> routePanel();
            case BUILD -> buildPanel();
            case MARKET -> marketPanel();
            case AUTO -> automation();
            case MACROS -> macros();
            case BINDS -> binds();
            case DEPOSIT -> deposit();
            case VISUAL -> visuals();
            case PERF -> performance();
            case SETTINGS -> settings();
        }
    }

    private void home() {
        int x = cx + 12, y = cy + 47;
        int w = Math.max(90, (cw - 34) / 2);
        button("OPEN MINING", x, y, w, 32, Tone.PRIMARY, b -> { tab = Tab.MINING; build(); });
        button("AUTOMATION", x + w + 10, y, w, 32, Tone.NORMAL, b -> { tab = Tab.AUTO; build(); });
        button("MACRO RECORDER", x, y + 44, w, 32, Tone.NORMAL, b -> { tab = Tab.MACROS; build(); });
        button("DISPLAY / HUD", x + w + 10, y + 44, w, 32, Tone.NORMAL, b -> { tab = Tab.VISUAL; build(); });
        button("STOP ALL", x, y + 104, w * 2 + 10, 34, Tone.DANGER, b -> emergencyStop());
    }

    private void emergencyStop() {
        if (Recorder.active) Recorder.stop();
        if (Replayer.active) Replayer.stop();
        Deposit.stop();
        if (Digger.active) Digger.stop();
        Binds.clear();
        Digger.releaseKeys(Minecraft.getInstance());
        build();
    }

    private boolean splitMining() { return cw >= 430; }
    private int miningLeftWidth() { return splitMining() ? (cw - 34) / 2 : cw - 24; }

    private void mining() {
        Minecraft mc = Minecraft.getInstance();
        int x = cx + 12, y = cy + 30;
        int gap = 7;
        int buttonsW = cw - 24;
        int third = (buttonsW - gap * 2) / 3;
        // Always visible at the top, even at a small GUI scale.
        button("START", x, y, third, 29, Tone.PRIMARY, b -> {
            if (Digger.active && Digger.userPaused()) Digger.togglePause();
            else if (!Digger.active) Digger.start(mc);
            if (Digger.active && !Digger.isPaused()) onClose();
            else build();
        });
        button(Digger.userPaused() ? "RESUME" : "PAUSE", x + third + gap, y,
                third, 29, Tone.NORMAL, b -> { Digger.togglePause(); build(); });
        button("STOP", x + (third + gap) * 2, y, third, 29, Tone.DANGER, b -> { Digger.stop(); build(); });

        int w = miningLeftWidth();
        int half = Math.max(47, (w - 5) / 2);
        button("POS1 LOOK", x, y + 75, half, 23, Tone.NORMAL,
                b -> callAndRefresh(() -> Digger.setSavedLookedPoint(1, lookedPos)));
        button("POS1 FEET", x + half + 5, y + 75, half, 23, Tone.NORMAL,
                b -> callAndRefresh(() -> Digger.setPoint(mc, 1)));
        button("POS2 LOOK", x, y + 129, half, 23, Tone.NORMAL,
                b -> callAndRefresh(() -> Digger.setSavedLookedPoint(2, lookedPos)));
        button("POS2 FEET", x + half + 5, y + 129, half, 23, Tone.NORMAL,
                b -> callAndRefresh(() -> Digger.setPoint(mc, 2)));

        button("DEPTH -", x, y + 182, half, 23, Tone.MUTED,
                b -> callAndRefresh(() -> Settings.I.digDepth = Math.max(1, Settings.I.digDepth - 1)));
        button("DEPTH +", x + half + 5, y + 182, half, 23, Tone.MUTED,
                b -> callAndRefresh(() -> Settings.I.digDepth = Math.min(8, Settings.I.digDepth + 1)));
        button("TURN -", x, y + 211, half, 23, Tone.MUTED,
                b -> callAndRefresh(() -> Settings.I.digTurnSpeed = Math.max(2, Settings.I.digTurnSpeed - 1)));
        button("TURN +", x + half + 5, y + 211, half, 23, Tone.MUTED,
                b -> callAndRefresh(() -> Settings.I.digTurnSpeed = Math.min(20, Settings.I.digTurnSpeed + 1)));
        if (y + 266 < cy + ch) {
            button("CLEAR SELECTION", x, y + 244, w, 23, Tone.DANGER,
                    b -> callAndRefresh(Digger::clearSelection));
        }
    }

    private void routePanel() {
        Settings s = Settings.I;
        int x = cx + 12, y = cy + 41, w = Math.max(90, Math.min(340, cw - 24));
        button(toggle("3D SELECTION OUTLINE", s.selectionVisuals), x, y, w, 26, Tone.NORMAL,
                b -> callAndRefresh(() -> s.selectionVisuals = !s.selectionVisuals));
        button(toggle("ACTIVE LAYER", s.showActiveLayer), x, y + 33, w, 26, Tone.NORMAL,
                b -> callAndRefresh(() -> s.showActiveLayer = !s.showActiveLayer));
        button(toggle("DRILL 3x3x2 PREVIEW", s.drillPreview), x, y + 66, w, 26, Tone.NORMAL,
                b -> callAndRefresh(() -> s.drillPreview = !s.drillPreview));
        button(toggle("SAFE ROUTE LINES", s.showRoute), x, y + 99, w, 26, Tone.NORMAL,
                b -> callAndRefresh(() -> s.showRoute = !s.showRoute));
        button("MINE " + (s.digUp ? "UP" : "DOWN") + "  [SWITCH]", x, y + 132, w, 27, Tone.SELECTED,
                b -> callAndRefresh(() -> s.digUp = !s.digUp));
        int half = Math.max(44, (w - 6) / 2);
        button("X -", x, y + 204, half, 24, Tone.MUTED, b -> resizeAxis(0, -1));
        button("X +", x + half + 6, y + 204, half, 24, Tone.MUTED, b -> resizeAxis(0, 1));
        button("Z -", x, y + 233, half, 24, Tone.MUTED, b -> resizeAxis(2, -1));
        button("Z +", x + half + 6, y + 233, half, 24, Tone.MUTED, b -> resizeAxis(2, 1));
    }

    private void resizeAxis(int axis, int delta) {
        if (Digger.active || Settings.I.dig2 == null) {
            Recorder.msg("Stop mining and set POS2 to resize");
            return;
        }
        Settings.I.dig2[axis] += delta;
        Settings.save();
        build();
    }

    private void buildPanel() {
        Settings s = Settings.I;
        int x = cx + 12, y = cy + 44, w = Math.max(90, Math.min(340, cw - 24));
        button(toggle("BUILD HOLOGRAM", s.buildPreview), x, y, w, 29, Tone.PRIMARY,
                b -> callAndRefresh(() -> s.buildPreview = !s.buildPreview));
        button("SHAPE: " + new String[]{"FLOOR", "WALLS", "FILL"}[s.buildMode % 3],
                x, y + 38, w, 29, Tone.NORMAL,
                b -> callAndRefresh(() -> s.buildMode = (s.buildMode + 1) % 3));
        button("OPEN SELECTION", x, y + 83, w, 27, Tone.NORMAL, b -> { tab = Tab.MINING; build(); });
        button("EXPAND X", x, y + 127, w, 25, Tone.MUTED, b -> resizeAxis(0, 1));
        button("EXPAND Z", x, y + 158, w, 25, Tone.MUTED, b -> resizeAxis(2, 1));
    }

    private void marketPanel() {
        Settings s = Settings.I;
        int x = cx + 12, y = cy + 38, w = Math.max(90, Math.min(380, cw - 24));
        String[] ores = ResourceCatalog.NAMES;
        int cellW = Math.max(62, (w - 8) / 2);
        for (int i = 0; i < ores.length; i++) {
            final int idx = i;
            int row = i / 2, col = i % 2;
            button((s.resourceFilter[i] ? "[x] " : "[ ] ") + ores[i] + " " + ResourceCatalog.count(Minecraft.getInstance(),i), x + col * (cellW + 8), y + row * 31,
                    cellW, 25, s.resourceFilter[i] ? Tone.SELECTED : Tone.MUTED,
                    b -> callAndRefresh(() -> s.resourceFilter[idx] = !s.resourceFilter[idx]));
        }
        button("PRICE - 50", x, y + 155, cellW, 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.marketPrice = Math.max(1, s.marketPrice - 50)));
        button("PRICE + 50", x + cellW + 8, y + 155, cellW, 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.marketPrice = Math.min(100000000, s.marketPrice + 50)));
        button(toggle("INVENTORY ORE HUD", s.resourceHud), x, y + 250, w, 25, Tone.NORMAL,
                b -> callAndRefresh(() -> s.resourceHud = !s.resourceHud));
        EditBox price = new EditBox(font, x, y + 187, w, 18, Component.literal("Exact sale price"));
        price.setMaxLength(9);
        price.setValue(String.valueOf(s.marketPrice));
        price.setResponder(value -> {
            try {
                int entered = Integer.parseInt(value);
                if (entered > 0 && entered <= 100000000) s.marketPrice = entered;
            } catch (NumberFormatException ignored) {
                // Keep last valid value while the user is editing.
            }
        });
        addRenderableWidget(price);
        button("COPY /market sell " + s.marketPrice, x, y + 213, w, 30, Tone.PRIMARY, b -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player.getMainHandItem().getCount() < 64) {
                Recorder.msg("Hold a complete 64-stack first");
                return;
            }
            mc.keyboardHandler.setClipboard("/market sell " + s.marketPrice);
            Recorder.msg("Command copied. Check price and confirm sale manually.");
        });
    }

    private void automation() {
        Settings s = Settings.I;
        int x = cx + 14, y = cy + 40, w = Math.min(330, cw - 28);
        button(toggle("AUTO TOOL", s.autoTool), x, y, w, 29, Tone.NORMAL,
                b -> callAndRefresh(() -> s.autoTool = !s.autoTool));
        button(toggle("AUTO REFILL", s.autoRefill), x, y + 37, w, 29, Tone.NORMAL,
                b -> callAndRefresh(() -> s.autoRefill = !s.autoRefill));
        button(toggle("AUTO EAT", s.autoEat), x, y + 74, w, 29, Tone.NORMAL,
                b -> callAndRefresh(() -> s.autoEat = !s.autoEat));
        button("-", x, y + 121, 38, 25, Tone.MUTED,
                b -> callAndRefresh(() -> s.eatBelow = Math.max(1, s.eatBelow - 1)));
        button("+", x + 116, y + 121, 38, 25, Tone.MUTED,
                b -> callAndRefresh(() -> s.eatBelow = Math.min(19, s.eatBelow + 1)));
        button(toggle("AUTO DEPOSIT", s.autoDeposit), x, y + 173, w, 27, Tone.NORMAL,
                b -> callAndRefresh(() -> s.autoDeposit = !s.autoDeposit));
        button("CONFIGURE DEPOSIT", x, y + 207, w, 25, Tone.MUTED,
                b -> { tab = Tab.DEPOSIT; build(); });
    }

    private void macros() {
        List<String> names = Macros.list();
        if (selectedMacro != null && !names.contains(selectedMacro)) selectedMacro = null;
        int listW = Math.min(232, Math.max(110, cw / 2 - 10));
        int x = cx + 9, y = cy + 38;
        int rows = Math.max(2, Math.min(8, (ch - 98) / 28));
        macroPage = Math.max(0, Math.min(macroPage, Math.max(0, (names.size() - 1) / rows)));
        for (int i = 0; i < rows; i++) {
            int idx = macroPage * rows + i;
            if (idx >= names.size()) break;
            String m = names.get(idx);
            button((m.equals(selectedMacro) ? "* " : "") + cut(m, 22), x, y + i * 28,
                    listW, 23, m.equals(selectedMacro) ? Tone.SELECTED : Tone.NORMAL,
                    b -> { selectedMacro = m; rename = ""; build(); });
        }
        int bottom = cy + ch - 28;
        button("<", x, bottom, 36, 22, Tone.MUTED, b -> { macroPage = Math.max(0, macroPage - 1); build(); });
        button(">", x + listW - 36, bottom, 36, 22, Tone.MUTED, b -> { macroPage++; build(); });
        int rx = x + listW + 10;
        int rw = Math.max(55, cx + cw - rx - 10);
        button(Recorder.active ? "STOP RECORD" : "RECORD", rx, y, rw, 28, Tone.PRIMARY, b -> {
            if (Recorder.active) { Recorder.stop(); build(); }
            else { onClose(); Recorder.start(); }
        });
        if (selectedMacro == null) return;
        String chosen = selectedMacro;
        button("PLAY ONCE", rx, y + 39, rw, 24, Tone.NORMAL,
                b -> { onClose(); Replayer.play(Macros.load(chosen), false); });
        button("LOOP", rx, y + 69, rw, 24, Tone.NORMAL,
                b -> { onClose(); Replayer.play(Macros.load(chosen), true); });
        button("STOP", rx, y + 99, rw, 24, Tone.DANGER,
                b -> { Replayer.stop(); if (Recorder.active) Recorder.stop(); build(); });
        int k = Settings.I.macroKeys.getOrDefault(chosen, -1);
        button(keyLabel("macro", "KEY: ", k), rx, y + 130, rw, 24, Tone.MUTED,
                b -> capture("macro", code -> {
                    if (code < 0) Settings.I.macroKeys.remove(chosen);
                    else Settings.I.macroKeys.put(chosen, code);
                }));
        if (rw >= 90) {
            EditBox e = new EditBox(font, rx, y + 163, rw, 19, Component.literal("Rename macro"));
            e.setMaxLength(32);
            e.setValue(rename.isEmpty() ? chosen : rename);
            e.setResponder(v -> rename = v);
            addRenderableWidget(e);
            button("RENAME", rx, y + 187, rw, 23, Tone.MUTED, b -> {
                String n = Macros.sanitize(rename.isEmpty() ? chosen : rename);
                if (!n.isEmpty() && Macros.rename(chosen, n)) selectedMacro = n;
                rename = "";
                build();
            });
            if (ch > 310) button("DELETE MACRO", rx, y + 217, rw, 24, Tone.DANGER,
                    b -> { Macros.delete(chosen); selectedMacro = null; build(); });
        }
    }

    private void binds() {
        Settings s = Settings.I;
        int x = cx + 10, y = cy + 36;
        int rows = Math.max(2, Math.min(7, (ch - 85) / 30));
        bindsPage = Math.max(0, Math.min(bindsPage, Math.max(0, (s.binds.size() - 1) / rows)));
        for (int i = 0; i < rows; i++) {
            int idx = bindsPage * rows + i;
            if (idx >= s.binds.size()) break;
            Settings.Bind bd = s.binds.get(idx);
            int yy = y + i * 30;
            button(keyLabel("bind" + idx, "", bd.key), x, yy, 92, 24, Tone.NORMAL,
                    b -> capture("bind" + idx, code -> bd.key = code));
            EditBox box = new EditBox(font, x + 99, yy + 2, Math.max(40, cw - 154), 19, Component.literal("Commands"));
            box.setMaxLength(200);
            box.setValue(bd.lines == null ? "" : bd.lines);
            box.setResponder(v -> bd.lines = v);
            addRenderableWidget(box);
            button("X", cx + cw - 39, yy, 24, 24, Tone.DANGER,
                    b -> callAndRefresh(() -> s.binds.remove(bd)));
        }
        int bottom = cy + ch - 28;
        button("+ ADD BIND", x, bottom, 128, 24, Tone.PRIMARY, b -> callAndRefresh(() -> {
            s.binds.add(new Settings.Bind());
            bindsPage = (s.binds.size() - 1) / rows;
        }));
        button("<", cx + cw - 88, bottom, 34, 24, Tone.MUTED,
                b -> { bindsPage = Math.max(0, bindsPage - 1); build(); });
        button(">", cx + cw - 47, bottom, 34, 24, Tone.MUTED,
                b -> { bindsPage++; build(); });
    }

    private void deposit() {
        Settings s = Settings.I;
        int x = cx + 12, y = cy + 44, w = cw - 24;
        button(toggle("AUTO DEPOSIT", s.autoDeposit), x, y, Math.min(w, 300), 27, Tone.NORMAL,
                b -> callAndRefresh(() -> s.autoDeposit = !s.autoDeposit));
        button("ADD LOOKED CHEST", x, y + 36, Math.min(w, 170), 25, Tone.NORMAL,
                b -> callAndRefresh(() -> { if (lookedPos != null) s.chests.add(lookedPos.clone()); }));
        button("CLEAR CHESTS (" + s.chests.size() + ")", x + Math.min(w, 178), y + 36,
                Math.max(66, w - 180), 25, Tone.DANGER,
                b -> callAndRefresh(s.chests::clear));
        EditBox before = new EditBox(font, x, y + 98, w, 20, Component.literal("Command when full"));
        before.setMaxLength(200); before.setValue(s.fullCmd); before.setResponder(v -> s.fullCmd = v);
        addRenderableWidget(before);
        EditBox after = new EditBox(font, x, y + 151, w, 20, Component.literal("Return command"));
        after.setMaxLength(200); after.setValue(s.backCmd); after.setResponder(v -> s.backCmd = v);
        addRenderableWidget(after);
        int gridWidth = Math.min(w, 9 * 24);
        int gap = gridWidth / 9;
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++) {
            final int slot = row == 0 ? col : 9 + (row - 1) * 9 + col;
            int yy = y + 212 + row * 23;
            if (yy + 20 >= cy + ch - 2) continue;
            button(s.keepSlots[slot] ? "K" : "+", x + col * gap, yy, gap - 2, 20,
                    s.keepSlots[slot] ? Tone.SELECTED : Tone.MUTED,
                    b -> callAndRefresh(() -> s.keepSlots[slot] = !s.keepSlots[slot]));
        }
    }

    private void visuals() {
        Settings s = Settings.I;
        int x = cx + 12, y = cy + 36;
        int w = Math.min(cw - 24, 345);
        button("WORLD", x, y, Math.max(60, (w - 8) / 2), 25,
                visualPage == 0 ? Tone.SELECTED : Tone.MUTED, b -> { visualPage = 0; build(); });
        button("HUD / PREVIEW", x + (w - 8) / 2 + 8, y, Math.max(60, (w - 8) / 2), 25,
                visualPage == 1 ? Tone.SELECTED : Tone.MUTED, b -> { visualPage = 1; build(); });
        int yy = y + 39;
        if (visualPage == 0) {
            button(toggle("FULLBRIGHT", s.brightMax), x, yy, w, 26, Tone.NORMAL,
                    b -> callAndRefresh(() -> s.brightMax = !s.brightMax));
            button(toggle("HIDE RAIN (LOCAL)", s.noRain), x, yy + 33, w, 26, Tone.NORMAL,
                    b -> callAndRefresh(() -> s.noRain = !s.noRain));
            button("TIME: " + Extras.TIME_NAMES[s.timeMode], x, yy + 66, w, 26, Tone.NORMAL,
                    b -> callAndRefresh(() -> s.timeMode = (s.timeMode + 1) % Extras.TIME_NAMES.length));
            button(toggle("AUTO SPRINT", s.autoSprint), x, yy + 99, w, 26, Tone.NORMAL,
                    b -> callAndRefresh(() -> s.autoSprint = !s.autoSprint));
            button(toggle("CRITICAL SOUND", s.critSound), x, yy + 132, w, 26, Tone.NORMAL,
                    b -> callAndRefresh(() -> s.critSound = !s.critSound));
            button(keyLabel("zoom", "ZOOM KEY: ", s.zoomKey), x, yy + 165, w, 26, Tone.MUTED,
                    b -> capture("zoom", code -> s.zoomKey = code));
        } else {
            button(toggle("SHULKER PREVIEW", s.shulkerPreview), x, yy, w, 26, Tone.NORMAL,
                    b -> callAndRefresh(() -> s.shulkerPreview = !s.shulkerPreview));
            button(toggle("COORDINATES HUD", s.hudCoords), x, yy + 33, w, 26, Tone.NORMAL,
                    b -> callAndRefresh(() -> s.hudCoords = !s.hudCoords));
            button(toggle("ARMOR DURABILITY", s.hudArmor), x, yy + 66, w, 26, Tone.NORMAL,
                    b -> callAndRefresh(() -> s.hudArmor = !s.hudArmor));
            button(toggle("WAYPOINT HUD", s.hudWaypoints), x, yy + 99, w, 26, Tone.NORMAL,
                    b -> callAndRefresh(() -> s.hudWaypoints = !s.hudWaypoints));
            button("ADD WAYPOINT HERE", x, yy + 132, w, 26, Tone.NORMAL, b -> callAndRefresh(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    Settings.Waypoint point = new Settings.Waypoint();
                    point.name = "Point " + (s.waypoints.size() + 1);
                    point.x = (int) Math.floor(mc.player.getX());
                    point.y = (int) Math.floor(mc.player.getY());
                    point.z = (int) Math.floor(mc.player.getZ());
                    s.waypoints.add(point);
                }
            }));
            button("CLEAR WAYPOINTS (" + s.waypoints.size() + ")", x, yy + 165, w, 26, Tone.DANGER,
                    b -> callAndRefresh(s.waypoints::clear));
        }
    }

    private void performance() {
        Settings s = Settings.I;
        int x = cx + 12, y = cy + 38, w = Math.min(cw - 24, 340);
        button(toggle("DISTANT PLAYER CULLING", s.optOn), x, y, w, 27, Tone.NORMAL,
                b -> callAndRefresh(() -> s.optOn = !s.optOn));
        button("CROWD -", x, y + 53, Math.max(60, (w - 8) / 2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.optCrowd = Math.max(0, s.optCrowd - 5)));
        button("CROWD +", x + (w - 8) / 2 + 8, y + 53, Math.max(60, (w - 8) / 2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.optCrowd = Math.min(100, s.optCrowd + 5)));
        button("DISTANCE -", x, y + 111, Math.max(60, (w - 8) / 2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.optDist = Math.max(0, s.optDist - 8)));
        button("DISTANCE +", x + (w - 8) / 2 + 8, y + 111, Math.max(60, (w - 8) / 2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.optDist = Math.min(128, s.optDist + 8)));
        button(toggle("CULLING ZONE", s.optZone), x, y + 152, w, 26, Tone.NORMAL,
                b -> callAndRefresh(() -> s.optZone = !s.optZone));
        button("ZONE POS1", x, y + 186, Math.max(60, (w - 8) / 2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> Optimize.setZonePoint(Minecraft.getInstance(), 1)));
        button("ZONE POS2", x + (w - 8) / 2 + 8, y + 186, Math.max(60, (w - 8) / 2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> Optimize.setZonePoint(Minecraft.getInstance(), 2)));
    }

    private void settings() {
        Settings s = Settings.I;
        int x = cx + 12, y = cy + 38, w = Math.min(cw - 24, 350);
        button(keyLabel("open", "OPEN MENU: ", s.openKey), x, y, w, 27, Tone.NORMAL,
                b -> capture("open", code -> s.openKey = code < 0 ? 345 : code));
        button(keyLabel("record", "RECORD KEY: ", s.recordKey), x, y + 37, w, 27, Tone.NORMAL,
                b -> capture("record", code -> s.recordKey = code));
        button(keyLabel("stop", "STOP ALL: ", s.stopKey), x, y + 74, w, 27, Tone.DANGER,
                b -> capture("stop", code -> s.stopKey = code));
        button(toggle("LOW HEALTH ALARM", s.hpAlarm), x, y + 116, w, 27, Tone.NORMAL,
                b -> callAndRefresh(() -> s.hpAlarm = !s.hpAlarm));
        button("HEALTH -", x, y + 151, Math.max(60, (w - 8) / 2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.hpHearts = Math.max(1, s.hpHearts - 1)));
        button("HEALTH +", x + (w - 8) / 2 + 8, y + 151, Math.max(60, (w - 8) / 2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.hpHearts = Math.min(9, s.hpHearts + 1)));
        button("WINDOW WIDTH -", x, y + 199, Math.max(60,(w-8)/2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.uiWidthPercent = Math.max(55,s.uiWidthPercent-5)));
        button("WINDOW WIDTH +", x + (w-8)/2 + 8, y + 199, Math.max(60,(w-8)/2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.uiWidthPercent = Math.min(96,s.uiWidthPercent+5)));
        button("WINDOW HEIGHT -", x, y + 231, Math.max(60,(w-8)/2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.uiHeightPercent = Math.max(65,s.uiHeightPercent-5)));
        button("WINDOW HEIGHT +", x + (w-8)/2 + 8, y + 231, Math.max(60,(w-8)/2), 26, Tone.MUTED,
                b -> callAndRefresh(() -> s.uiHeightPercent = Math.min(97,s.uiHeightPercent+5)));
        if (ch > 335) button("SAVE SETTINGS", x, y + 264, w, 26, Tone.PRIMARY,
                b -> { Settings.save(); Recorder.msg("Settings saved"); });
    }

    private String keyLabel(String id, String prefix, int key) {
        return id.equals(captureId) ? "PRESS A KEY..." : prefix + (key < 0 ? "NOT SET" : Keys.name(key));
    }

    private void capture(String id, Consumer<Integer> receiver) {
        captureId = id;
        capture = receiver;
        build();
    }

    private static String toggle(String name, boolean on) { return name + (on ? "   [ON]" : "   [OFF]"); }
    private static String cut(String s, int len) { return s.length() > len ? s.substring(0, len - 2) + ".." : s; }
    private static String point(int[] p) { return p == null || p.length < 3 ? "NOT SET" : p[0] + "  /  " + p[1] + "  /  " + p[2]; }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (capture != null) {
            Consumer<Integer> handler = capture;
            capture = null;
            captureId = null;
            if (key != 256) {
                handler.accept(key == 259 || key == 261 ? -1 : key);
                Settings.save();
            }
            build();
            return true;
        }
        if (key == Settings.I.openKey && !(getFocused() instanceof EditBox)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xE0060A12);
        rounded(g, px - 2, py - 2, pw + 4, ph + 4, 0xFF33475D);
        rounded(g, px, py, pw, ph, BG);
        rounded(g, px + 4, py + 4, side - 2, ph - 8, PANEL);
        g.fillGradient(px + side + 2, py + 46, px + pw - 3, py + 48, GOLD, TEAL);
        g.fill(px + 8, py + ph - 34, px + side - 3, py + ph - 33, 0xFF3E4D53);
        rounded(g, cx, cy, cw, ch, PANEL);
        g.fill(cx, cy, cx + cw, cy + 2, 0xFF273F55);
        if (tab == Tab.MINING) {
            int x = cx + 6, y = cy + 4;
            int h = Math.min(248, ch - 8);
            if (h > 40) drawCard(g, x, y, cw - 12, h);
        }
    }

    private void renderSelectionGrid(GuiGraphics g, int x, int y, int w, int h) {
        rounded(g, x, y, w, h, 0xFF101724);
        int[] a = Settings.I.dig1, b = Settings.I.dig2;
        if (a == null || b == null || a.length < 3 || b.length < 3) {
            g.drawCenteredString(font, "SELECT POS1 + POS2", x + w / 2, y + h / 2 - 4, GREY);
            return;
        }
        int cols = (int) Math.min(19, Math.abs((long) a[0] - b[0]) + 1);
        int rows = (int) Math.min(10, Math.abs((long) a[2] - b[2]) + 1);
        int cell = Math.max(2, Math.min((w - 10) / cols, (h - 10) / rows));
        int ox = x + (w - cols * cell) / 2;
        int oy = y + (h - rows * cell) / 2;
        for (int row = 0; row < rows; row++) for (int col = 0; col < cols; col++) {
            int sx = ox + col * cell, sy = oy + row * cell;
            g.fill(sx, sy, sx + cell - 1, sy + cell - 1, (row % 2 == 0 ? 0xFF3D978F : 0xFF356C81));
        }
        g.fill(ox, oy, ox + Math.max(2, cell - 1), oy + Math.max(2, cell - 1), GOLD);
    }

    private void drawPage(GuiGraphics g) {
        int x = cx + 13, y = cy + 7;
        switch (tab) {
            case HOME -> {
                line(g, "CONTROL CENTER", x, y + 5, TEAL);
                line(g, "Your real installed tools, in one place.", x, y + 22, GREY);
                line(g, "MINING: " + Digger.status(), x, y + 218, Digger.active ? GOLD : GREY);
                line(g, "RECORDER: " + (Recorder.active ? "RECORDING" : "IDLE"), x, y + 232, Recorder.active ? RED : GREY);
                line(g, "REPLAY: " + (Replayer.active ? "ACTIVE" : "IDLE"), x, y + 245, GREY);
            }
            case MINING -> {
                line(g, "MINING STUDIO  /  SELECT & PREVIEW", x, y, TEAL);
                int ly = cy + 30;
                int leftW = miningLeftWidth();
                line(g, "POINT 1 / " + cut(point(Settings.I.dig1), Math.max(12, leftW / 6)), x, ly + 61, WHITE);
                line(g, "POINT 2 / " + cut(point(Settings.I.dig2), Math.max(12, leftW / 6)), x, ly + 116, WHITE);
                line(g, "DEPTH: " + Settings.I.digDepth + " LAYERS", x, ly + 168, GOLD);
                line(g, "TURN: " + Settings.I.digTurnSpeed + " DEG/TICK", x, ly + 238, TEAL);
                if (splitMining()) {
                    int rightX = x + leftW + 12;
                    int rightW = cw - 26 - leftW - 12;
                    line(g, "AREA / TOP VIEW", rightX + 5, ly + 46, PURPLE);
                    renderSelectionGrid(g, rightX, ly + 60, rightW, 115);
                    line(g, "COLUMNS " + Digger.selectedCells(), rightX + 5, ly + 185, GREY);
                    line(g, "STATUS: " + cut(Digger.status(), Math.max(12, rightW / 6)), rightX + 5, ly + 199,
                            Digger.active ? TEAL : GREY);
                    int barY = ly + 223;
                    rounded(g, rightX + 4, barY, rightW - 8, 8, 0xFF34445A);
                    rounded(g, rightX + 4, barY,
                            (rightW - 8) * Digger.progressPercent() / 100, 8, TEAL);
                    line(g, Digger.progressPercent() + "%  /  " + Digger.completedCells() + "/" + Digger.totalCells(), rightX + 5, barY + 14, GREY);
                } else if (ch >= 340) {
                    line(g, "STATUS: " + cut(Digger.status(), Math.max(15, cw / 6)), x, ly + 279, GREY);
                }
                if (!Digger.lastError().isEmpty() && ch >= 390) {
                    line(g, cut(Digger.lastError(), Math.max(30, cw / 6)), x, cy + ch - 18, RED);
                }
            }
            case ROUTE -> {
                line(g, "SMART ROUTE  /  3D GIZMOS", x, y, TEAL);
                line(g, "Overlay: native Minecraft lines, no X-Ray.", x, y + 25, GREY);
                line(g, "Route: safe horizontal A* / avoid gaps.", x, y + 162, GREY);
                line(g, "Mining direction: " + (Settings.I.digUp ? "UP" : "DOWN"), x, y + 183, GOLD);
            }
            case BUILD -> {
                line(g, "BUILD STUDIO  /  HOLOGRAM", x, y, BLUE);
                line(g, "Preview only: no block placement packets.", x, y + 27, GREY);
                line(g, "Mode: " + new String[]{"FLOOR","WALLS","FILL"}[Settings.I.buildMode % 3], x, y + 108, GOLD);
            }
            case MARKET -> {
                line(g, "RESOURCE FILTER  /  MARKET ASSISTANT", x, y, TEAL);
                line(g, "Only loaded inventory items; no ore ESP.", x, y + 22, GREY);
                line(g, "Price: " + Settings.I.marketPrice + "  |  Exact price below.", x, y + 188, GOLD);
                line(g, "Hold 64 items; confirm in chat manually.", x, y + 285, GREY);
            }
            case AUTO -> {
                line(g, "AUTOMATION / INSTALLED MODULES", x, y, TEAL);
                line(g, "Food threshold: " + Settings.I.eatBelow + " / 20", x, y + 149, GOLD);
                line(g, "Only real modules have ON/OFF controls.", x, y + 253, GREY);
                line(g, "Auto Fish / Farm / Build are not installed yet.", x, y + 267, GREY);
            }
            case MACROS -> {
                line(g, "MACRO LIBRARY", x, y, TEAL);
                if (Macros.list().isEmpty()) line(g, "No recordings yet. Use RECORD.", x, y + 55, GREY);
            }
            case BINDS -> line(g, "COMMAND BINDS / KEY -> COMMAND", x, y, TEAL);
            case DEPOSIT -> {
                line(g, "INVENTORY / AUTO DEPOSIT", x, y, TEAL);
                line(g, "Command when inventory is full", x, y + 125, GREY);
                line(g, "Command after depositing", x, y + 178, GREY);
                line(g, "K = keep    + = deposit", x, y + 243, GREY);
            }
            case VISUAL -> {
                line(g, "VISUAL & HUD / REAL TOGGLES", x, y, TEAL);
                line(g, "Client-side options only", x, y + 265, GREY);
            }
            case PERF -> {
                line(g, "PERFORMANCE / LIGHTWEIGHT RENDER", x, y, TEAL);
                line(g, "Crowd threshold: " + Settings.I.optCrowd + (Settings.I.optCrowd == 0 ? " (disabled)" : " players"), x, y + 85, GOLD);
                line(g, "Distance threshold: " + Settings.I.optDist + (Settings.I.optDist == 0 ? " (disabled)" : " blocks"), x, y + 143, GOLD);
                line(g, "Sodium stays a separate optional mod.", x, y + 247, GREY);
            }
            case SETTINGS -> {
                line(g, "SETTINGS / KEYBOARD & SAFETY", x, y, TEAL);
                line(g, "Health warning at " + Settings.I.hpHearts + " hearts", x, y + 189, GOLD);
                line(g, "Right Ctrl opens this window by default.", x, y + 260, GREY);
                if (captureId != null) line(g, "PRESS A KEY; ESC CANCELS", x, y + 274, GOLD);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        rounded(g, px + 11, py + 12, 23, 24, 0xFF29392F);
        line(g, "M", px + 19, py + 20, GOLD);
        line(g, "MACRO STUDIO  //  OBSIDIAN", px + side + 15, py + 16, WHITE);
        line(g, "ATELIER UI  /  1.21.11", px + side + 15, py + 29, GOLD);
        line(g, Recorder.active ? "REC" : Digger.active ? (Digger.isPaused() ? "PAUSED" : "MINING") : "READY",
                px + pw - 110, py + 19, Recorder.active ? RED : Digger.active ? TEAL : GREY);
        line(g, "v2.3.0 PREVIEW", px + 15, py + ph - 20, GOLD);
        drawPage(g);
        // Custom skinned clickable widgets (native buttons are visually transparent).
        for (UiButton ui : paintedButtons) {
            Button b = ui.button();
            int x = b.getX(), y = b.getY(), w = b.getWidth(), h = b.getHeight();
            boolean hot = b.isHoveredOrFocused();
            int bg = switch (ui.tone()) {
                case PRIMARY -> hot ? 0xFF89F3D0 : 0xFF168F70;
                case DANGER -> hot ? 0xFFFF8498 : 0xFF8B354D;
                case SELECTED -> hot ? 0xFF41677B : 0xFF2A5066;
                case MUTED -> hot ? 0xFF34445E : 0xFF202C40;
                case NORMAL -> hot ? 0xFF2D6A65 : 0xFF243C47;
            };
            rounded(g, x, y, w, h, hot ? 0xFF9AD5DE : BORDER);
            rounded(g, x + 1, y + 1, w - 2, h - 2, bg);
            if (ui.tone() == Tone.SELECTED) g.fill(x + 1, y + 3, x + 3, y + h - 3, TEAL);
            int fg = (ui.tone() == Tone.PRIMARY && hot) ? 0xFF10262D : WHITE;
            g.drawCenteredString(font, cut(ui.caption(), Math.max(4, (w - 10) / 6)), x + w / 2, y + (h - 8) / 2, fg);
        }
    }
}
