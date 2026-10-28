package dev.sunshine.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag-to-position editor for Sunshine HUD widgets, including the sprint indicator. */
public final class SunshineHudEditorScreen extends Screen {
    private static final int GOLD = 0xFFFFC857;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB8C0CC;
    private final Screen parent;
    private String active;
    private boolean resizing;
    private int dragX, dragY;

    public SunshineHudEditorScreen(Screen parent) {
        super(Component.literal("Sunshine HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // GUI scale changes alter the available logical dimensions. Keep saved widgets
        // within the new viewport instead of letting them jump off-screen.
        SunshineConfig.hudX = clamp(pos(SunshineConfig.hudX, 6), 0, Math.max(0, width - SunshineConfig.hudW));
        SunshineConfig.hudY = clamp(pos(SunshineConfig.hudY, 6), 0, Math.max(0, height - SunshineConfig.hudH));
        SunshineConfig.coordsX = clamp(pos(SunshineConfig.coordsX, 6), 0, Math.max(0, width - SunshineConfig.coordsW));
        SunshineConfig.coordsY = clamp(pos(SunshineConfig.coordsY, 56), 0, Math.max(0, height - SunshineConfig.coordsH));
        SunshineConfig.keysX = clamp(pos(SunshineConfig.keysX, width - SunshineConfig.keysW - 6), 0, Math.max(0, width - SunshineConfig.keysW));
        SunshineConfig.keysY = clamp(pos(SunshineConfig.keysY, height - SunshineConfig.keysH - 32), 0, Math.max(0, height - SunshineConfig.keysH));
        int sprintW = sprintWidth();
        int sprintH = sprintHeight();
        SunshineConfig.sprintX = clamp(pos(SunshineConfig.sprintX, (width - sprintW) / 2), 0, Math.max(0, width - sprintW));
        SunshineConfig.sprintY = clamp(pos(SunshineConfig.sprintY, height / 2 + 24), 0, Math.max(0, height - sprintH));
        if (SunshineConfig.clockX < 0) SunshineConfig.clockX = Math.max(6, width - 70);
        if (SunshineConfig.potionX < 0) SunshineConfig.potionX = Math.max(6, width - 150);
        if (SunshineConfig.armorX < 0) SunshineConfig.armorX = Math.max(6, width / 2 - 90);
        if (SunshineConfig.armorY < 0) SunshineConfig.armorY = Math.max(6, height - 58);
        if (SunshineConfig.attackCooldownX < 0) SunshineConfig.attackCooldownX = Math.max(6, width / 2 - 55);
        if (SunshineConfig.attackCooldownY < 0) SunshineConfig.attackCooldownY = Math.max(6, height / 2 + 42);
        if (SunshineConfig.targetInfoX < 0) SunshineConfig.targetInfoX = 6;
        if (SunshineConfig.targetInfoY < 0) SunshineConfig.targetInfoY = 92;
        SunshineConfig.save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x88000000);
        g.fill(12, 12, width - 12, 44, 0xE00D1117);
        g.fill(12, 12, width - 12, 14, GOLD);
        g.text(font, Component.literal("SUNSHINE  /  HUD EDITOR").withStyle(s -> s.withColor(GOLD).withBold(true)), 22, 22, WHITE, true);
        g.text(font, Component.literal("Drag to move • drag gold corner to resize • saves automatically"), 22, 33, MUTED, true);

        int hx = SunshineConfig.hudX >= 0 ? SunshineConfig.hudX : 6;
        int hy = SunshineConfig.hudY >= 0 ? SunshineConfig.hudY : 6;
        int cw = SunshineConfig.coordsX >= 0 ? SunshineConfig.coordsX : 6;
        int ch = SunshineConfig.coordsY >= 0 ? SunshineConfig.coordsY : 56;
        int kw = SunshineConfig.keysX >= 0 ? SunshineConfig.keysX : width - 70;
        int kh = SunshineConfig.keysY >= 0 ? SunshineConfig.keysY : height - 90;
        drawWidget(g, "hud", "FPS / CPS PANEL", hx, hy, SunshineConfig.hudW, SunshineConfig.hudH, mouseX, mouseY);
        drawWidget(g, "coords", "COORDINATES", cw, ch, SunshineConfig.coordsW, SunshineConfig.coordsH, mouseX, mouseY);
        drawWidget(g, "keys", "KEYSTROKES", kw, kh, SunshineConfig.keysW, SunshineConfig.keysH, mouseX, mouseY);
        int sx = pos(SunshineConfig.sprintX, (width - sprintWidth()) / 2);
        int sy = pos(SunshineConfig.sprintY, height / 2 + 24);
        drawWidget(g, "sprint", "SPRINTING INDICATOR", sx, sy, sprintWidth(), sprintHeight(), mouseX, mouseY);
        drawWidget(g, "clock", "CLOCK", pos(SunshineConfig.clockX, width - 70), pos(SunshineConfig.clockY, 6), 74, 18, mouseX, mouseY);
        drawWidget(g, "direction", "DIRECTION", SunshineConfig.directionX, SunshineConfig.directionY, 112, 18, mouseX, mouseY);
        drawWidget(g, "session", "SESSION TIMER", SunshineConfig.sessionX, SunshineConfig.sessionY, 142, 18, mouseX, mouseY);
        drawWidget(g, "potion", "POTION EFFECTS + TIME", pos(SunshineConfig.potionX, width - 150), pos(SunshineConfig.potionY, 8), 146, 42, mouseX, mouseY);
        drawWidget(g, "armor", "ARMOR DURABILITY", pos(SunshineConfig.armorX, width / 2 - 90), pos(SunshineConfig.armorY, height - 58), 108, 56, mouseX, mouseY);
        drawWidget(g, "attack", "ATTACK COOLDOWN", pos(SunshineConfig.attackCooldownX, width / 2 - 55), pos(SunshineConfig.attackCooldownY, height / 2 + 42), 112, 20, mouseX, mouseY);
        drawWidget(g, "target", "TARGET INFO", pos(SunshineConfig.targetInfoX, 6), pos(SunshineConfig.targetInfoY, 92), 150, 30, mouseX, mouseY);

        int bw = 82, bh = 22, bx = width - bw - 18, by = 18;
        int rw = 112, rx = bx - rw - 8;
        g.fill(rx, by, rx + rw, by + bh, 0xFF20242B);
        g.outline(rx, by, rw, bh, 0xFFB8C0CC);
        g.centeredText(font, Component.literal("RESET LAYOUT"), rx + rw / 2, by + 7, WHITE);
        g.fill(bx, by, bx + bw, by + bh, 0xFF20242B);
        g.outline(bx, by, bw, bh, GOLD);
        g.centeredText(font, Component.literal("DONE").withStyle(s -> s.withColor(GOLD).withBold(true)), bx + bw / 2, by + 7, WHITE);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawWidget(GuiGraphicsExtractor g, String id, String label, int x, int y, int w, int h, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        int edge = id.equals(active) || hover ? GOLD : 0xFFB8C0CC;
        g.fill(x, y, x + w, y + h, 0xCC111720);
        g.outline(x, y, w, h, edge);
        g.fill(x, y, x + w, y + 2, GOLD);
        // Visible bottom-right resize grip. Keep it large enough for touch/mouse precision.
        g.fill(x + w - 20, y + h - 20, x + w, y + h, GOLD);
        g.text(font, Component.literal(label).withStyle(s -> s.withColor(WHITE).withBold(true)), x + 5, y + 5, WHITE, true);
        if (id.equals("hud")) {
            g.text(font, "FPS  " + Minecraft.getInstance().getFps(), x + 5, y + 17, WHITE, true);
            g.text(font, "CPS  " + SunshineClient.getLeftCps() + " | " + SunshineClient.getRightCps(), x + 5, y + 28, WHITE, true);
        } else if (id.equals("coords")) {
            Minecraft mc = Minecraft.getInstance();
            String xyz = mc.player == null ? "XYZ  -- / -- / --" : String.format(java.util.Locale.ROOT, "XYZ  %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            g.text(font, xyz, x + 5, y + 5, WHITE, true);
        } else if (id.equals("keys")) {
            g.text(font, "W", x + 27, y + 17, WHITE, true);
            g.text(font, "A  S  D", x + 9, y + 29, WHITE, true);
            g.text(font, "SPACE", x + 12, y + 43, WHITE, true);
        } else if (id.equals("sprint")) {
            g.fill(x + 5, y + 18, x + w - 5, y + h - 4, SunshineConfig.sprintBackground ? 0xC00D1117 : 0x00000000);
            g.fill(x + 5, y + 18, x + 7, y + h - 4, SunshineConfig.sprintColor);
            g.text(font, net.minecraft.network.chat.Component.literal(
                    SunshineConfig.sprintText == null || SunshineConfig.sprintText.isBlank() ? "Sprinting" : SunshineConfig.sprintText)
                    .withStyle(st -> st.withColor(SunshineConfig.sprintColor).withBold(true)), x + 11, y + 22, WHITE, true);
        } else if (id.equals("clock")) g.text(font, "12:45:30", x + 5, y + 5, GOLD, true);
        else if (id.equals("direction")) g.text(font, "DIR  NW  315°", x + 5, y + 5, WHITE, true);
        else if (id.equals("session")) g.text(font, "SESSION  00:12:34", x + 5, y + 5, WHITE, true);
        else if (id.equals("potion")) { g.text(font, "Speed       1:24", x + 5, y + 17, WHITE, true); g.text(font, "Strength   0:42", x + 5, y + 29, WHITE, true); }
        else if (id.equals("armor")) { g.text(font, "HELM  84%", x + 5, y + 17, WHITE, true); g.text(font, "CHEST 61%", x + 5, y + 29, WHITE, true); }
        else if (id.equals("attack")) { g.fill(x + 5, y + 15, x + w - 5, y + 18, 0xFF343A43); g.fill(x + 5, y + 15, x + w - 25, y + 18, GOLD); }
        else if (id.equals("target")) { g.text(font, "TARGET  Zombie", x + 5, y + 17, WHITE, true); g.text(font, "Distance 4.2m", x + 5, y + 28, MUTED, true); }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        int bw = 82, bh = 22, bx = width - bw - 18, by = 18;
        if (inside(mx, my, bx, by, bw, bh)) { onClose(); return true; }
        int rw = 112, rx = bx - rw - 8;
        if (inside(mx, my, rx, by, rw, bh)) {
            SunshineConfig.hudX = 6; SunshineConfig.hudY = 6;
            SunshineConfig.coordsX = 6; SunshineConfig.coordsY = 56;
            SunshineConfig.keysX = -1; SunshineConfig.keysY = -1;
            SunshineConfig.hudW = 112; SunshineConfig.hudH = 46;
            SunshineConfig.coordsW = 160; SunshineConfig.coordsH = 22;
            SunshineConfig.keysW = 120; SunshineConfig.keysH = 82;
            SunshineConfig.sprintX = -1; SunshineConfig.sprintY = -1;
            SunshineConfig.clockX = -1; SunshineConfig.clockY = 6;
            SunshineConfig.directionX = 6; SunshineConfig.directionY = 48;
            SunshineConfig.sessionX = 6; SunshineConfig.sessionY = 69;
            SunshineConfig.potionX = -1; SunshineConfig.potionY = 8;
            SunshineConfig.armorX = -1; SunshineConfig.armorY = -1;
            SunshineConfig.attackCooldownX = -1; SunshineConfig.attackCooldownY = -1;
            SunshineConfig.targetInfoX = 6; SunshineConfig.targetInfoY = 92;
            SunshineConfig.save();
            active = null; resizing = false;
            return true;
        }
        String[] ids = {"sprint", "keys", "coords", "hud", "clock", "direction", "session", "potion", "armor", "attack", "target"};
        int[] xs = {pos(SunshineConfig.sprintX, (width - sprintWidth()) / 2), pos(SunshineConfig.keysX, width - SunshineConfig.keysW - 6), pos(SunshineConfig.coordsX, 6), pos(SunshineConfig.hudX, 6), pos(SunshineConfig.clockX, width - 70), SunshineConfig.directionX, SunshineConfig.sessionX, pos(SunshineConfig.potionX, width - 150), pos(SunshineConfig.armorX, width / 2 - 90), pos(SunshineConfig.attackCooldownX, width / 2 - 55), pos(SunshineConfig.targetInfoX, 6)};
        int[] ys = {pos(SunshineConfig.sprintY, height / 2 + 24), pos(SunshineConfig.keysY, height - SunshineConfig.keysH - 32), pos(SunshineConfig.coordsY, 56), pos(SunshineConfig.hudY, 6), pos(SunshineConfig.clockY, 6), SunshineConfig.directionY, SunshineConfig.sessionY, pos(SunshineConfig.potionY, 8), pos(SunshineConfig.armorY, height - 58), pos(SunshineConfig.attackCooldownY, height / 2 + 42), pos(SunshineConfig.targetInfoY, 92)};
        int[] ws = {sprintWidth(), SunshineConfig.keysW, SunshineConfig.coordsW, SunshineConfig.hudW, 74, 112, 142, 146, 108, 112, 150};
        int[] hs = {sprintHeight(), SunshineConfig.keysH, SunshineConfig.coordsH, SunshineConfig.hudH, 18, 18, 18, 42, 56, 20, 30};
        for (int i = 0; i < ids.length; i++) {
            if (!inside(mx, my, xs[i], ys[i], ws[i], hs[i])) continue;
            active = ids[i];
            resizing = (ids[i].equals("hud") || ids[i].equals("coords") || ids[i].equals("keys")) && mx >= xs[i] + ws[i] - 24 && my >= ys[i] + hs[i] - 24;
            dragX = (int) mx; dragY = (int) my;
            return true;
        }
        active = null; resizing = false;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (active == null) return super.mouseDragged(event, dx, dy);
        int mx = (int) event.x(), my = (int) event.y();
        // In this Minecraft GUI API event.x/y may remain at the press origin during
        // dragging. The supplied deltas are the reliable per-frame movement values.
        int deltaX = (int) Math.round(dx), deltaY = (int) Math.round(dy);
        if (deltaX == 0 && deltaY == 0) {
            deltaX = mx - dragX;
            deltaY = my - dragY;
        }
        if (resizing) {
            if (active.equals("hud")) { SunshineConfig.hudW = clamp(SunshineConfig.hudW + deltaX, 100, Math.max(100, Math.min(360, width))); SunshineConfig.hudH = clamp(SunshineConfig.hudH + deltaY, 36, Math.max(36, Math.min(180, height))); }
            else if (active.equals("coords")) { SunshineConfig.coordsW = clamp(SunshineConfig.coordsW + deltaX, 100, Math.max(100, Math.min(360, width))); SunshineConfig.coordsH = clamp(SunshineConfig.coordsH + deltaY, 18, Math.max(18, Math.min(100, height))); }
            else { SunshineConfig.keysW = clamp(SunshineConfig.keysW + deltaX, 60, Math.max(60, Math.min(220, width))); SunshineConfig.keysH = clamp(SunshineConfig.keysH + deltaY, 50, Math.max(50, Math.min(220, height))); }
        } else {
            if (active.equals("sprint")) { SunshineConfig.sprintX = clamp(pos(SunshineConfig.sprintX, (width - sprintWidth()) / 2) + deltaX, 0, Math.max(0, width - sprintWidth())); SunshineConfig.sprintY = clamp(pos(SunshineConfig.sprintY, height / 2 + 24) + deltaY, 0, Math.max(0, height - sprintHeight())); }
            else if (active.equals("clock")) { SunshineConfig.clockX = clamp(pos(SunshineConfig.clockX, width - 70) + deltaX, 0, Math.max(0, width - 74)); SunshineConfig.clockY = clamp(pos(SunshineConfig.clockY, 6) + deltaY, 0, Math.max(0, height - 18)); }
            else if (active.equals("direction")) { SunshineConfig.directionX = clamp(SunshineConfig.directionX + deltaX, 0, Math.max(0, width - 112)); SunshineConfig.directionY = clamp(SunshineConfig.directionY + deltaY, 0, Math.max(0, height - 18)); }
            else if (active.equals("session")) { SunshineConfig.sessionX = clamp(SunshineConfig.sessionX + deltaX, 0, Math.max(0, width - 142)); SunshineConfig.sessionY = clamp(SunshineConfig.sessionY + deltaY, 0, Math.max(0, height - 18)); }
            else if (active.equals("potion")) { SunshineConfig.potionX = clamp(pos(SunshineConfig.potionX, width - 150) + deltaX, 0, Math.max(0, width - 146)); SunshineConfig.potionY = clamp(pos(SunshineConfig.potionY, 8) + deltaY, 0, Math.max(0, height - 130)); }
            else if (active.equals("armor")) { SunshineConfig.armorX = clamp(pos(SunshineConfig.armorX, width / 2 - 90) + deltaX, 0, Math.max(0, width - 108)); SunshineConfig.armorY = clamp(pos(SunshineConfig.armorY, height - 58) + deltaY, 0, Math.max(0, height - 64)); }
            else if (active.equals("attack")) { SunshineConfig.attackCooldownX = clamp(pos(SunshineConfig.attackCooldownX, width / 2 - 55) + deltaX, 0, Math.max(0, width - 112)); SunshineConfig.attackCooldownY = clamp(pos(SunshineConfig.attackCooldownY, height / 2 + 42) + deltaY, 0, Math.max(0, height - 20)); }
            else if (active.equals("target")) { SunshineConfig.targetInfoX = clamp(pos(SunshineConfig.targetInfoX, 6) + deltaX, 0, Math.max(0, width - 150)); SunshineConfig.targetInfoY = clamp(pos(SunshineConfig.targetInfoY, 92) + deltaY, 0, Math.max(0, height - 30)); }
            else if (active.equals("hud")) { SunshineConfig.hudX = clamp(pos(SunshineConfig.hudX, 6) + deltaX, 0, Math.max(0, width - SunshineConfig.hudW)); SunshineConfig.hudY = clamp(pos(SunshineConfig.hudY, 6) + deltaY, 0, Math.max(0, height - SunshineConfig.hudH)); }
            else if (active.equals("coords")) { SunshineConfig.coordsX = clamp(pos(SunshineConfig.coordsX, 6) + deltaX, 0, Math.max(0, width - SunshineConfig.coordsW)); SunshineConfig.coordsY = clamp(pos(SunshineConfig.coordsY, 56) + deltaY, 0, Math.max(0, height - SunshineConfig.coordsH)); }
            else { SunshineConfig.keysX = clamp(pos(SunshineConfig.keysX, width - SunshineConfig.keysW - 6) + deltaX, 0, Math.max(0, width - SunshineConfig.keysW)); SunshineConfig.keysY = clamp(pos(SunshineConfig.keysY, height - SunshineConfig.keysH - 32) + deltaY, 0, Math.max(0, height - SunshineConfig.keysH)); }
        }
        dragX = mx; dragY = my;
        SunshineConfig.save();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (active != null) { active = null; resizing = false; return true; }
        return super.mouseReleased(event);
    }

    private int sprintWidth() {
        String label = SunshineConfig.sprintText == null || SunshineConfig.sprintText.isBlank() ? "Sprinting" : SunshineConfig.sprintText;
        return Math.max(72, font.width(label) * SunshineConfig.sprintTextSize + 22);
    }

    private int sprintHeight() { return 12 * SunshineConfig.sprintTextSize + 8; }

    private static int pos(int value, int fallback) { return value >= 0 ? value : fallback; }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { SunshineConfig.save(); Minecraft.getInstance().gui.setScreen(parent); }
}
