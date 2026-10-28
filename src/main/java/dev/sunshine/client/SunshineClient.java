package dev.sunshine.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class SunshineClient implements ClientModInitializer {
    public static final String MOD_ID = "sunshine";
    static final int GOLD = 0xFFFFC857;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB8C0CC;

    private static int leftCps, rightCps, lastLeftCps, lastRightCps;
    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static long lastClickAt;
    private static boolean leftWasDown, rightWasDown;
    private static boolean leftHeld, rightHeld;
    private static boolean leftEventDown, rightEventDown;
    private static long leftPressedAt, rightPressedAt;
    private static final float[] keyAnim = new float[5];
    private static long lastHudFrame = System.nanoTime();
    private static long sessionStartedAt = System.currentTimeMillis();
    private static boolean sprintToggled = false;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    static KeyMapping sprintToggleKey;
    static KeyMapping menuKey;
    private static KeyMapping hudKey, coordsKey, keysKey, freelookKey;
    private static boolean freelookActive = false;
    private static float freelookYaw, freelookPitch;

    @Override
    public void onInitializeClient() {
        SunshineConfig.load();
        menuKey = register("key.sunshine.menu", SunshineConfig.menuBind);
        sprintToggleKey = register("key.sunshine.toggle_sprint", SunshineConfig.sprintBind);
        hudKey = register("key.sunshine.hud", SunshineConfig.hudBind);
        coordsKey = register("key.sunshine.coords", SunshineConfig.coordsBind);
        keysKey = register("key.sunshine.keystrokes", SunshineConfig.keysBind);
        freelookKey = register("key.sunshine.freelook", SunshineConfig.freelookBind);

        ClientTickEvents.END_CLIENT_TICK.register(SunshineClient::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "main_hud"), (g, delta) -> renderHud(g));

        // Touch-friendly entry point (Android / MJLauncher users may not have Right Shift)
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof PauseScreen) {
                Screens.getWidgets(screen).add(Button.builder(Component.literal("Sunshine"),
                        b -> client.gui.setScreen(new SunshineScreen(screen))).bounds(6, 6, 74, 20).build());
            }
            if (screen instanceof TitleScreen) {
                // Use a complete Sunshine title screen, not vanilla's blue panorama with recolored buttons.
                client.gui.setScreen(new SunshineTitleScreen());
            }
        });
    }

    static void applyBindings() {
        setBinding(menuKey, SunshineConfig.menuBind);
        setBinding(hudKey, SunshineConfig.hudBind);
        setBinding(coordsKey, SunshineConfig.coordsBind);
        setBinding(keysKey, SunshineConfig.keysBind);
        setBinding(sprintToggleKey, SunshineConfig.sprintBind);
        setBinding(freelookKey, SunshineConfig.freelookBind);
    }

    private static void setBinding(KeyMapping mapping, int key) {
        if (mapping != null) mapping.setKey(key < 0 ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate(key));
    }

    private static KeyMapping register(String name, int key) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, InputConstants.Type.KEYSYM, key < 0 ? InputConstants.UNKNOWN.getValue() : key, CATEGORY));
    }

    private static void tick(Minecraft client) {
        while (freelookKey.consumeClick()) {
            if (client.player != null) {
                if (!freelookActive) {
                    freelookYaw = client.player.getYRot();
                    freelookPitch = client.player.getXRot();
                    freelookActive = true;
                } else {
                    freelookActive = false;
                }
                SunshineConfig.freelookEnabled = freelookActive;
                SunshineConfig.save();
            }
        }
        if (client.player == null || client.level == null) freelookActive = false;

        while (sprintToggleKey.consumeClick()) { sprintToggled = !sprintToggled; SunshineConfig.toggleSprint = sprintToggled; SunshineConfig.save(); }
        if (client.player != null && SunshineConfig.toggleSprint && sprintToggled && client.player.getFoodData().getFoodLevel() > 6 && !client.player.isCrouching()) client.player.setSprinting(true);
        while (menuKey.consumeClick()) {
            client.gui.setScreen(client.gui.screen() == null ? new SunshineScreen(null) : null);
        }
        while (hudKey.consumeClick()) toggleHud();
        while (coordsKey.consumeClick()) toggleCoords();
        while (keysKey.consumeClick()) toggleKeystrokes();

        long now = System.currentTimeMillis();
        if (client.getWindow() != null && client.getWindow().handle() != 0L) {
            long handle = client.getWindow().handle();
            boolean leftDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rightDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            leftWasDown = leftDown;
            rightWasDown = rightDown;
            leftHeld = leftDown;
            rightHeld = rightDown;
        }
        // Rolling one-second window: each press is counted once, rather than resetting on a fixed tick.
        trimClicks(leftClicks, now);
        trimClicks(rightClicks, now);
        leftCps = leftClicks.size();
        rightCps = rightClicks.size();
        if (leftCps > 0) lastLeftCps = leftCps;
        if (rightCps > 0) lastRightCps = rightCps;
        // Keep the last non-zero reading visible briefly after the final click.
        if (now - lastClickAt <= 2000) {
            if (leftCps == 0) leftCps = lastLeftCps;
            if (rightCps == 0) rightCps = lastRightCps;
        } else {
            leftCps = rightCps = lastLeftCps = lastRightCps = 0;
        }
    }

    /** Records every native mouse press, including very fast double-clicks that polling can miss. */
    public static void recordMouseEvent(int button, int action) {
        long now = System.currentTimeMillis();
        if (action == 1) {
            recordMouseClick(button);
            if (button == 0) { leftEventDown = true; leftPressedAt = now; }
            else if (button == 1) { rightEventDown = true; rightPressedAt = now; }
        } else if (action == 0) {
            if (button == 0) leftEventDown = false;
            else if (button == 1) rightEventDown = false;
        }
    }

    /** Held state for the LMB/RMB boxes: real press/release events, shown for at least 90 ms so a fast click is visible. */
    private static boolean visuallyHeld(boolean eventDown, long pressedAt) {
        return eventDown || System.currentTimeMillis() - pressedAt < 90;
    }

    public static void recordMouseClick(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) leftClicks.addLast(now);
        else if (button == 1) rightClicks.addLast(now);
        else return;
        lastClickAt = now;
    }

    private static void trimClicks(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() >= 1000) clicks.removeFirst();
    }

    private static void renderHud(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        long nowNs = System.nanoTime();
        float dt = Math.min(0.1f, (nowNs - lastHudFrame) / 1_000_000_000f);
        lastHudFrame = nowNs;
        if (mc.player == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int defaultY = 6;
        if (SunshineConfig.hud) defaultY = renderInfoPanel(g, mc, SunshineConfig.hudX >= 0 ? SunshineConfig.hudX : 6,
                SunshineConfig.hudY >= 0 ? SunshineConfig.hudY : defaultY) + 4;
        int cx = SunshineConfig.coordsX >= 0 ? SunshineConfig.coordsX : 6;
        int cy = SunshineConfig.coordsY >= 0 ? SunshineConfig.coordsY : defaultY;
        if (SunshineConfig.coords) renderCoords(g, mc, cx, cy);
        if (SunshineConfig.keystrokes) renderKeystrokes(g, mc, sw, sh, dt);
        if (SunshineConfig.mouseVisualizer) renderMouseVisualizer(g, mc, sw, sh);
        if (SunshineConfig.sprintIndicator && mc.options.keySprint.isDown()) renderSprintIndicator(g, mc, sw, sh);
        if (SunshineConfig.clockHud) renderClock(g, mc, sw, sh);
        if (SunshineConfig.directionHud) renderDirection(g, mc, sw, sh);
        if (SunshineConfig.sessionTimerHud) renderSessionTimer(g, mc, sw, sh);
        if (SunshineConfig.potionHud) renderPotionHud(g, mc, sw, sh);
        if (SunshineConfig.armorHud) renderArmorHud(g, mc, sw, sh);
        if (SunshineConfig.attackCooldownHud) renderAttackCooldown(g, mc, sw, sh);
        if (SunshineConfig.targetInfoHud) renderTargetInfo(g, mc, sw, sh);
    }

    private static int renderInfoPanel(GuiGraphicsExtractor g, Minecraft mc, int x, int y) {
        String fps = "FPS  " + mc.getFps();
        String cps = "CPS  " + leftCps + " | " + rightCps;
        String hint = "RSHIFT menu · F6 F7 F8";
        int w = Math.max(Math.max(80, Math.max(mc.font.width(fps), mc.font.width(cps)) + 14), SunshineConfig.hudW);
        if (SunshineConfig.hints) w = Math.max(w, mc.font.width(hint) + 14);
        int h = Math.max(SunshineConfig.hudH, SunshineConfig.hints ? 46 : 36);
        int panelColor = SunshineConfig.performanceMode == 0 ? 0xE00D1117 : (SunshineConfig.performanceMode == 1 ? 0xA00D1117 : 0x700D1117);
        g.fill(x, y, x + w, y + h, panelColor);
        if (SunshineConfig.performanceMode != 2) g.fill(x, y, x + w, y + 2, GOLD);
        int contentW = Math.max(1, w - 12);
        float textScale = Math.max(0.75f, Math.min(2.0f, contentW / (float)Math.max(1, mc.font.width("FPS  999 | CPS  99 | 99"))));
        g.pose().pushMatrix();
        g.pose().translate(x + 6, y + 5);
        g.pose().scale(textScale, textScale);
        g.text(mc.font, Component.literal("SUNSHINE").withStyle(s -> s.withColor(GOLD).withBold(true)), 0, 0, WHITE, true);
        g.text(mc.font, fps, 0, 11, WHITE, true);
        g.text(mc.font, cps, 0, 21, WHITE, true);
        if (SunshineConfig.hints && h >= 44) g.text(mc.font, hint, 0, 31, MUTED, true);
        g.pose().popMatrix();
        return y + h;
    }


    private static void renderClock(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        int x = SunshineConfig.clockX >= 0 ? SunshineConfig.clockX : Math.max(6, sw - mc.font.width(time) - 12);
        int y = SunshineConfig.clockY >= 0 ? SunshineConfig.clockY : 6;
        if (SunshineConfig.performanceMode == 0) {
            g.fill(x - 5, y - 3, x + mc.font.width(time) + 5, y + 13, 0xAA11151D);
            g.fill(x - 5, y - 3, x + mc.font.width(time) + 5, y - 1, GOLD);
            g.outline(x - 5, y - 3, mc.font.width(time) + 10, 16, 0xFFFFC857);
        } else if (SunshineConfig.performanceMode == 1) {
            g.fill(x - 3, y - 2, x + mc.font.width(time) + 3, y + 11, 0x6611151D);
        }
        g.text(mc.font, Component.literal(time), x, y, GOLD);
    }

    private static void renderSprintIndicator(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        String label = SunshineConfig.sprintText == null || SunshineConfig.sprintText.isBlank() ? "Sprinting" : SunshineConfig.sprintText;
        int scale = SunshineConfig.sprintTextSize;
        int textW = mc.font.width(label) * scale;
        int w = textW + 16;
        int h = 12 * scale + 4;
        int x = SunshineConfig.sprintX >= 0 ? SunshineConfig.sprintX : (sw - w) / 2;
        int y = SunshineConfig.sprintY >= 0 ? SunshineConfig.sprintY : sh / 2 + 24;
        x = Math.max(0, Math.min(x, Math.max(0, sw - w)));
        y = Math.max(0, Math.min(y, Math.max(0, sh - h)));
        if (SunshineConfig.sprintBackground) g.fill(x, y, x + w, y + h, 0xC00D1117);
        g.fill(x, y, x + 2, y + h, SunshineConfig.sprintColor);
        g.pose().pushMatrix();
        g.pose().translate(x + 7, y + 2);
        g.pose().scale(scale, scale);
        g.text(mc.font, Component.literal(label).withStyle(st -> st.withColor(SunshineConfig.sprintColor).withBold(true)), 0, 0, WHITE, true);
        g.pose().popMatrix();
    }

    private static void renderCoords(GuiGraphicsExtractor g, Minecraft mc, int x, int y) {
        String coords = String.format("XYZ  %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        int w = mc.font.width(coords) + 12;
        g.fill(x, y, x + w, y + 14, 0xA00D1117);
        g.fill(x, y, x + 2, y + 14, GOLD);
        g.text(mc.font, coords, x + 6, y + 3, WHITE, true);
    }

    private static void renderKeystrokes(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh, float dt) {
        int widgetW = configuredKeysWidth();
        int widgetH = configuredKeysHeight();
        int bx = SunshineConfig.keysX >= 0 ? SunshineConfig.keysX : sw - widgetW - 6;
        int by = SunshineConfig.keysY >= 0 ? SunshineConfig.keysY : sh - widgetH - 6;
        // Hotbar occupies the centre 182px; on narrow screens lift the widget above hotbar + status bars.
        if (SunshineConfig.keysX < 0 && bx < sw / 2 + 91 + 4) by = sh - 22 - 44 - widgetH;
        if (by < 4) by = 4;
        int keyW = configuredKeyWidth();
        int gap = 2;
        int spaceH = configuredSpaceHeight();
        int totalW = configuredKeysWidth();
        int rowY = by + keyW + gap;
        boolean[] down = {mc.options.keyUp.isDown(), mc.options.keyLeft.isDown(), mc.options.keyDown.isDown(),
                mc.options.keyRight.isDown(), mc.options.keyJump.isDown()};
        for (int i = 0; i < down.length; i++) {
            keyAnim[i] = approach(keyAnim[i], down[i] ? 1f : 0f, dt * 12f);
        }
        drawKey(g, mc, "W", bx + keyW + gap, by, keyW, keyW, keyAnim[0]);
        drawKey(g, mc, "A", bx, rowY, keyW, keyW, keyAnim[1]);
        drawKey(g, mc, "S", bx + keyW + gap, rowY, keyW, keyW, keyAnim[2]);
        drawKey(g, mc, "D", bx + (keyW + gap) * 2, rowY, keyW, keyW, keyAnim[3]);
        drawKey(g, mc, "SPACE", bx, rowY + keyW + gap, totalW, spaceH, keyAnim[4]);
    }

    private static void renderDirection(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        if (mc.player == null) return;
        float yaw = mc.player.getYRot() % 360.0f;
        if (yaw < 0) yaw += 360.0f;
        String[] directions = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        String direction = directions[Math.round(yaw / 45.0f) % 8];
        String label = "DIR  " + direction + "  " + Math.round(yaw) + "°";
        int x = Math.max(0, SunshineConfig.directionX), y = Math.max(0, SunshineConfig.directionY);
        g.fill(x, y, x + mc.font.width(label) + 14, y + 18, 0xAA10141C);
        g.outline(x, y, mc.font.width(label) + 14, 18, 0xFFFFC857);
        g.text(mc.font, label, x + 7, y + 5, WHITE, false);
    }

    private static void renderSessionTimer(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        long seconds = Math.max(0, (System.currentTimeMillis() - sessionStartedAt) / 1000);
        long hours = seconds / 3600, minutes = (seconds % 3600) / 60, secs = seconds % 60;
        String label = String.format(java.util.Locale.ROOT, "SESSION  %02d:%02d:%02d", hours, minutes, secs);
        int x = Math.max(0, SunshineConfig.sessionX), y = Math.max(0, SunshineConfig.sessionY);
        g.fill(x, y, x + mc.font.width(label) + 14, y + 18, 0xAA10141C);
        g.outline(x, y, mc.font.width(label) + 14, 18, 0xFFFFC857);
        g.text(mc.font, label, x + 7, y + 5, WHITE, false);
    }

    private static int configuredKeyWidth() {
        return Math.max(12, (SunshineConfig.keysW - 4) / 3);
    }

    private static int configuredSpaceHeight() {
        int keyW = configuredKeyWidth();
        return Math.max(8, SunshineConfig.keysH - (keyW * 2 + 4));
    }

    private static int configuredKeysWidth() {
        int keyW = configuredKeyWidth();
        return keyW * 3 + 4;
    }

    private static int configuredKeysHeight() {
        int keyW = configuredKeyWidth();
        return keyW * 2 + 4 + configuredSpaceHeight();
    }

    private static void renderMouseVisualizer(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        int widgetW = configuredKeysWidth();
        int widgetH = configuredKeysHeight();
        int bx = SunshineConfig.keysX >= 0 ? SunshineConfig.keysX : sw - widgetW - 6;
        int by = SunshineConfig.keysY >= 0 ? SunshineConfig.keysY : sh - widgetH - 6;
        if (SunshineConfig.keysX < 0 && bx < sw / 2 + 95) by = sh - 22 - 44 - widgetH;
        // Derive the mouse row from the actual configurable keystrokes geometry.
        // Previously it used fixed constants, so resizing the widget made LMB/RMB
        // overlap the SPACE key and their buttons were shorter than the other keys.
        int y = by + widgetH + 3;
        int gap = 2;
        int w = (widgetW - gap) / 2;
        int h = configuredKeyWidth();
        if (SunshineConfig.leftMouse) drawMouseButton(g, mc, "LMB", bx, y, w, h, leftHeld || visuallyHeld(leftEventDown, leftPressedAt));
        if (SunshineConfig.rightMouse) drawMouseButton(g, mc, "RMB", bx + w + gap, y, w, h, rightHeld || visuallyHeld(rightEventDown, rightPressedAt));
    }

    private static void drawMouseButton(GuiGraphicsExtractor g, Minecraft mc, String label, int x, int y, int w, int h, boolean down) {
        int bg = down ? 0xFFFFC857 : 0xAA171D26;
        int fg = down ? 0xFF11151B : WHITE;
        int border = down ? 0xFFFFE58A : 0xFF596273;
        g.fill(x, y, x + w, y + h, bg);
        g.outline(x, y, w, h, border);
        int tx = x + (w - mc.font.width(label)) / 2;
        int ty = y + (h - 8) / 2 + 1;
        g.text(mc.font, label, tx, ty, fg, false);
    }

    private static void drawKey(GuiGraphicsExtractor g, Minecraft mc, String label, int x, int y, int w, int h, float t) {
        int bg = lerpColor(0xAA171D26, 0xDDFFC857, t);
        int fg = lerpColor(0xFFFFFFFF, 0xFF11151B, t);
        int border = lerpColor(0xFF596273, 0xFFFFE58A, t);
        g.fill(x, y, x + w, y + h, bg);
        g.outline(x, y, w, h, border);
        int tx = x + (w - mc.font.width(label)) / 2;
        int ty = y + (h - 8) / 2 + 1;
        g.text(mc.font, label, tx, ty, fg, false);
    }

    static float approach(float current, float target, float step) {
        if (current < target) return Math.min(target, current + step);
        return Math.max(target, current - step);
    }

    static int lerpColor(int a, int b, float t) {
        int r = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF, cb = (b >>> shift) & 0xFF;
            r |= (Math.round(ca + (cb - ca) * t) & 0xFF) << shift;
        }
        return r;
    }

    public static boolean isHudEnabled() { return SunshineConfig.hud; }
    public static boolean isCoordsEnabled() { return SunshineConfig.coords; }
    public static boolean isKeystrokesEnabled() { return SunshineConfig.keystrokes; }
    public static boolean isHintsEnabled() { return SunshineConfig.hints; }
    public static boolean isSprintIndicatorEnabled() { return SunshineConfig.sprintIndicator; }
    public static boolean isMouseVisualizerEnabled() { return SunshineConfig.mouseVisualizer; }
    public static boolean isLeftMouseEnabled() { return SunshineConfig.leftMouse; }
    public static boolean isRightMouseEnabled() { return SunshineConfig.rightMouse; }
    public static int getPerformanceMode() { return SunshineConfig.performanceMode; }
    public static int getLeftCps() { return leftCps; }
    public static int getRightCps() { return rightCps; }
    public static void toggleHud() { SunshineConfig.hud = !SunshineConfig.hud; SunshineConfig.save(); }
    public static void toggleCoords() { SunshineConfig.coords = !SunshineConfig.coords; SunshineConfig.save(); }
    public static void toggleKeystrokes() { SunshineConfig.keystrokes = !SunshineConfig.keystrokes; SunshineConfig.save(); }
    public static void toggleHints() { SunshineConfig.hints = !SunshineConfig.hints; SunshineConfig.save(); }
    public static void toggleSprintIndicator() { SunshineConfig.sprintIndicator = !SunshineConfig.sprintIndicator; SunshineConfig.save(); }
    public static void toggleClockHud() { SunshineConfig.clockHud = !SunshineConfig.clockHud; SunshineConfig.save(); }
    public static void toggleDirectionHud() { SunshineConfig.directionHud = !SunshineConfig.directionHud; SunshineConfig.save(); }
    public static void toggleSessionTimerHud() { SunshineConfig.sessionTimerHud = !SunshineConfig.sessionTimerHud; SunshineConfig.save(); }
    public static void toggleArmorHud() { SunshineConfig.armorHud = !SunshineConfig.armorHud; SunshineConfig.save(); }
    public static void toggleMouseVisualizer() { SunshineConfig.mouseVisualizer = !SunshineConfig.mouseVisualizer; SunshineConfig.save(); }
    public static void toggleLeftMouse() { SunshineConfig.leftMouse = !SunshineConfig.leftMouse; SunshineConfig.save(); }
    public static void toggleRightMouse() { SunshineConfig.rightMouse = !SunshineConfig.rightMouse; SunshineConfig.save(); }
    public static void cyclePerformanceMode() { SunshineConfig.performanceMode = SunshineConfig.performanceMode == 0 ? 1 : 0; SunshineConfig.save(); }
    private static void renderPotionHud(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        if (mc.player == null) return;
        int x = SunshineConfig.potionX >= 0 ? SunshineConfig.potionX : sw - 150;
        int y = SunshineConfig.potionY >= 0 ? SunshineConfig.potionY : 8, count = 0;
        for (var effect : mc.player.getActiveEffects()) {
            if (count >= 8) break;
            var instance = effect;
            String name = instance.getEffect().value().getDisplayName().getString();
            int seconds = Math.max(0, instance.getDuration() / 20);
            String time = seconds >= 3600 ? (seconds / 3600) + "h" : String.format("%d:%02d", seconds / 60, seconds % 60);
            String line = name + " " + time;
            int w = Math.min(146, mc.font.width(line) + 12);
            g.fill(x, y + count * 15, x + w, y + count * 15 + 13, 0xAA10141C);
            g.fill(x, y + count * 15, x + 2, y + count * 15 + 13, GOLD);
            g.text(mc.font, line, x + 6, y + count * 15 + 3, WHITE, true);
            count++;
        }
    }


    private static void renderArmorHud(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        if (mc.player == null) return;
        var armor = java.util.List.of(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET), mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS), mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST), mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        int x = SunshineConfig.armorX >= 0 ? SunshineConfig.armorX : Math.max(6, sw / 2 - 90);
        int y = SunshineConfig.armorY >= 0 ? SunshineConfig.armorY : Math.max(6, sh - 58);
        String[] labels = {"BOOTS", "LEGS", "CHEST", "HELM"};
        for (int i = 0; i < Math.min(4, armor.size()); i++) {
            var stack = armor.get(i);
            if (stack.isEmpty()) continue;
            int max = stack.getMaxDamage();
            int remaining = max <= 0 ? 100 : Math.max(0, (max - stack.getDamageValue()) * 100 / max);
            String line = labels[i] + " " + remaining + "%";
            int yy = y + i * 14;
            g.fill(x, yy, x + mc.font.width(line) + 12, yy + 12, 0xAA10141C);
            int color = remaining <= 20 ? 0xFFFF5555 : remaining <= 50 ? 0xFFFFC857 : GOLD;
            g.fill(x, yy, x + 2, yy + 12, color);
            g.text(mc.font, Component.literal(line).withStyle(st -> st.withColor(color)), x + 6, yy + 2, WHITE, true);
        }
    }


    private static void renderAttackCooldown(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        if (mc.player == null) return;
        float charge = Math.max(0.0f, Math.min(1.0f, mc.player.getAttackStrengthScale(0.0F)));
        String label = String.format(java.util.Locale.ROOT, "ATTACK  %d%%", Math.round(charge * 100));
        int x = SunshineConfig.attackCooldownX >= 0 ? SunshineConfig.attackCooldownX : Math.max(6, sw / 2 - 55);
        int y = SunshineConfig.attackCooldownY >= 0 ? SunshineConfig.attackCooldownY : Math.max(6, sh / 2 + 42);
        int w = Math.max(94, mc.font.width(label) + 12);
        g.fill(x, y, x + w, y + 19, 0xCC10141C); g.outline(x, y, w, 19, GOLD);
        g.fill(x + 4, y + 13, x + 4 + Math.round((w - 8) * charge), y + 16, charge >= 0.9f ? 0xFF77E69B : GOLD);
        g.text(mc.font, label, x + 6, y + 3, WHITE, true);
    }

    private static void renderTargetInfo(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        if (mc.player == null) return;
        net.minecraft.world.entity.Entity target = null;
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit) target = hit.getEntity();
        String name = target == null ? "TARGET  NONE" : "TARGET  " + target.getName().getString();
        String distance = target == null ? "Aim at an entity" : String.format(java.util.Locale.ROOT, "Distance %.1fm", mc.player.distanceTo(target));
        int w = Math.max(mc.font.width(name), mc.font.width(distance)) + 14;
        int x = SunshineConfig.targetInfoX >= 0 ? SunshineConfig.targetInfoX : 6;
        int y = SunshineConfig.targetInfoY >= 0 ? SunshineConfig.targetInfoY : 92;
        g.fill(x, y, x + w, y + 30, 0xCC10141C); g.fill(x, y, x + 2, y + 30, GOLD);
        g.text(mc.font, name, x + 7, y + 5, WHITE, true);
        g.text(mc.font, distance, x + 7, y + 17, MUTED, true);
    }

    public static void toggleAttackCooldownHud() { SunshineConfig.attackCooldownHud = !SunshineConfig.attackCooldownHud; SunshineConfig.save(); }
    public static void toggleTargetInfoHud() { SunshineConfig.targetInfoHud = !SunshineConfig.targetInfoHud; SunshineConfig.save(); }

    public static void togglePotionHud() {
        SunshineConfig.potionHud = !SunshineConfig.potionHud;
        SunshineConfig.save();
    }

    public static void toggleSelfNametag() {
        SunshineConfig.selfNametag = !SunshineConfig.selfNametag;
        SunshineConfig.save();
    }

    public static void toggleMotionBlur() {
        SunshineConfig.motionBlurEnabled = !SunshineConfig.motionBlurEnabled;
        SunshineConfig.save();
    }

    public static boolean isFreelookActive() { return freelookActive && SunshineConfig.freelookEnabled; }
    public static float getFreelookYaw() { return freelookYaw; }
    public static float getFreelookPitch() { return freelookPitch; }
    public static void rotateFreelook(double deltaX, double deltaY) {
        if (!isFreelookActive()) return;
        freelookYaw += (float) deltaX * 0.15F;
        freelookPitch = Math.max(-90.0F, Math.min(90.0F, freelookPitch + (float) deltaY * 0.15F));
    }

    public static void toggleFreelook() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (!freelookActive) {
            freelookYaw = client.player.getYRot();
            freelookPitch = client.player.getXRot();
            freelookActive = true;
        } else freelookActive = false;
        SunshineConfig.freelookEnabled = freelookActive;
        SunshineConfig.save();
    }

    public static void toggleSprintMode() {
        SunshineConfig.toggleSprint = !SunshineConfig.toggleSprint;
        if (!SunshineConfig.toggleSprint) sprintToggled = false;
        SunshineConfig.save();
    }



}
