package dev.sunshine.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Touch-friendly manager for Sunshine's own key bindings. */
public final class SunshineKeybindScreen extends Screen {
    private final Screen parent;
    private int waiting = -1;
    private String notice = "Click a binding, then press a key. ESC cancels.";
    private static final String[] NAMES = {"Open Sunshine menu", "Toggle HUD", "Toggle coordinates", "Toggle keystrokes", "Toggle sprint", "Toggle Freelook"};
    private static final int[] DEFAULTS = {GLFW.GLFW_KEY_RIGHT_SHIFT, GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_F8, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_L};
    private int[] values() { return new int[]{SunshineConfig.menuBind, SunshineConfig.hudBind, SunshineConfig.coordsBind, SunshineConfig.keysBind, SunshineConfig.sprintBind, SunshineConfig.freelookBind}; }
    public SunshineKeybindScreen(Screen parent) { super(Component.literal("Sunshine Keybinds")); this.parent = parent; }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xE8090C11);
        int pw = Math.min(460, width - 16), ph = Math.min(310, height - 16), x = (width - pw) / 2, y = (height - ph) / 2;
        g.fill(x, y, x + pw, y + ph, 0xF20D1118); g.outline(x, y, pw, ph, SunshineClient.GOLD); g.fill(x, y, x + pw, y + 3, SunshineClient.GOLD);
        g.text(font, Component.literal("SUNSHINE / KEYBINDS"), x + 14, y + 13, SunshineClient.GOLD, true);
        g.text(font, Component.literal("Click a row to rebind · RESET restores defaults · DEL unbinds"), x + 14, y + 29, 0xFFB8C0CC, false);
        int rowY = y + 49, rowH = 29, step = 34; int[] vals = values();
        for (int i = 0; i < NAMES.length; i++) { int ry = rowY + i * step;
            g.fill(x + 12, ry, x + pw - 12, ry + rowH, 0xFF191F28); g.outline(x + 12, ry, pw - 24, rowH, waiting == i ? SunshineClient.GOLD : 0xFF343D49);
            g.text(font, Component.literal(NAMES[i]), x + 20, ry + 10, 0xFFF4F6FA, false);
            String key = waiting == i ? "PRESS KEY..." : keyName(vals[i]); int keyX = x + pw - 132;
            g.fill(keyX, ry + 4, x + pw - 66, ry + rowH - 4, waiting == i ? 0xFFFFC857 : 0xFF292F39);
            g.text(font, Component.literal(key), keyX + 4, ry + 10, waiting == i ? 0xFF11151B : 0xFFFFD76A, false);
            g.fill(x + pw - 60, ry + 4, x + pw - 16, ry + rowH - 4, 0xFF30251A); g.text(font, Component.literal("RESET"), x + pw - 56, ry + 10, 0xFFFFD76A, false);
        }
        int fy = rowY + NAMES.length * step + 2; g.text(font, Component.literal(notice), x + 14, Math.min(y + ph - 27, fy), 0xFFB8C0CC, false);
        g.text(font, Component.literal("ESC / Close"), x + pw - 75, y + ph - 17, 0xFFFFD76A, false); super.extractRenderState(g, mouseX, mouseY, delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y(); int pw = Math.min(460, width - 16), ph = Math.min(310, height - 16), x = (width - pw) / 2, y = (height - ph) / 2;
        int rowY = y + 49, rowH = 29, step = 34;
        for (int i = 0; i < NAMES.length; i++) { int ry = rowY + i * step;
            if (mx >= x + pw - 60 && mx <= x + pw - 16 && my >= ry && my <= ry + rowH) { setValue(i, DEFAULTS[i]); waiting = -1; saveAndReport(); notice = "Reset to default."; return true; }
            if (mx >= x + 12 && mx <= x + pw - 66 && my >= ry && my <= ry + rowH) { waiting = i; notice = "Press a key. Backspace unbinds; ESC cancels."; return true; }
        } return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (waiting >= 0) { int key = event.key(); if (key == GLFW.GLFW_KEY_ESCAPE) { waiting = -1; notice = "Rebind cancelled."; return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) { setValue(waiting, -1); waiting = -1; saveAndReport(); return true; }
            if (key >= 0) { int[] vals = values(); for (int i = 0; i < vals.length; i++) if (i != waiting && vals[i] == key) { notice = "Conflict: already used by " + NAMES[i] + ". Choose another key."; return true; }
                setValue(waiting, key); waiting = -1; saveAndReport(); return true; } return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; } return super.keyPressed(event);
    }
    private void saveAndReport() { SunshineClient.applyBindings(); SunshineConfig.save(); notice = "Binding saved."; }
    private void setValue(int i, int v) { switch (i) { case 0 -> SunshineConfig.menuBind = v; case 1 -> SunshineConfig.hudBind = v; case 2 -> SunshineConfig.coordsBind = v; case 3 -> SunshineConfig.keysBind = v; case 4 -> SunshineConfig.sprintBind = v; case 5 -> SunshineConfig.freelookBind = v; default -> { } } }
    private static String keyName(int code) { if (code < 0) return "UNBOUND"; try { return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString().toUpperCase(java.util.Locale.ROOT); } catch (Exception e) { return "KEY " + code; } }
    @Override public void onClose() { waiting = -1; Minecraft.getInstance().gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
