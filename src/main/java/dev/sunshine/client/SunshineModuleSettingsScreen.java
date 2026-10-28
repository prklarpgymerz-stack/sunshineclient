package dev.sunshine.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Per-module settings page. Changes are persisted immediately. */
public final class SunshineModuleSettingsScreen extends Screen {
    private static final int GOLD = 0xFFFFD447;
    private final Screen parent;
    private final String module;
    private EditBox sprintText;
    private int left, top, panelW, panelH;

    public SunshineModuleSettingsScreen(Screen parent, String module) {
        super(Component.literal("Sunshine Module Settings"));
        this.parent = parent;
        this.module = module;
    }

    @Override protected void init() {
        panelW = Math.min(430, width - 20);
        panelH = Math.min(300, height - 20);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        int x = left + 18, w = panelW - 36, y = top + 76;
        if (module.equals("Sprint Indicator")) {
            sprintText = new EditBox(font, x, y, w, 20, Component.literal("Sprint text"));
            sprintText.setMaxLength(40); sprintText.setValue(SunshineConfig.sprintText);
            sprintText.setResponder(v -> { SunshineConfig.sprintText = v; SunshineConfig.save(); });
            addRenderableWidget(sprintText); y += 28;
            addRenderableWidget(Button.builder(Component.literal("Text size: " + (SunshineConfig.sprintTextSize == 1 ? "Normal" : "Large")), b -> {
                SunshineConfig.sprintTextSize = SunshineConfig.sprintTextSize == 1 ? 2 : 1; SunshineConfig.save(); b.setMessage(Component.literal("Text size: " + (SunshineConfig.sprintTextSize == 1 ? "Normal" : "Large")));
            }).bounds(x, y, w, 20).build()); y += 25;
            addRenderableWidget(Button.builder(Component.literal("Text color: " + colorName(SunshineConfig.sprintColor)), b -> {
                SunshineConfig.sprintColor = nextColor(SunshineConfig.sprintColor); SunshineConfig.save(); b.setMessage(Component.literal("Text color: " + colorName(SunshineConfig.sprintColor)));
            }).bounds(x, y, w, 20).build()); y += 25;
            addRenderableWidget(Button.builder(Component.literal("Background: " + (SunshineConfig.sprintBackground ? "ON" : "OFF")), b -> {
                SunshineConfig.sprintBackground = !SunshineConfig.sprintBackground; SunshineConfig.save(); b.setMessage(Component.literal("Background: " + (SunshineConfig.sprintBackground ? "ON" : "OFF")));
            }).bounds(x, y, w, 20).build());
        } else if (module.equals("Motion Blur")) {
            addRenderableWidget(Button.builder(Component.literal("Enabled: " + (SunshineConfig.motionBlurEnabled ? "ON" : "OFF")), b -> {
                SunshineConfig.motionBlurEnabled = !SunshineConfig.motionBlurEnabled; SunshineConfig.save();
                b.setMessage(Component.literal("Enabled: " + (SunshineConfig.motionBlurEnabled ? "ON" : "OFF")));
            }).bounds(x, y, w, 20).build()); y += 27;
            addRenderableWidget(new net.minecraft.client.gui.components.AbstractSliderButton(x, y, w, 20, Component.empty(), (SunshineConfig.motionBlurStrength - 1) / 99.0) {
                { updateMessage(); }
                @Override protected void updateMessage() { setMessage(Component.literal("Strength: " + SunshineConfig.motionBlurStrength + "%")); }
                @Override protected void applyValue() { SunshineConfig.motionBlurStrength = 1 + (int) Math.round(value * 99); }
                @Override public void onRelease(net.minecraft.client.input.MouseButtonEvent event) { super.onRelease(event); SunshineConfig.save(); }
            });
        } else if (module.equals("HUD Overlay")) {
            addRenderableWidget(Button.builder(Component.literal("HUD width: " + SunshineConfig.hudW), b -> { SunshineConfig.hudW = SunshineConfig.hudW >= 260 ? 100 : SunshineConfig.hudW + 20; SunshineConfig.save(); b.setMessage(Component.literal("HUD width: " + SunshineConfig.hudW)); }).bounds(x, y, w, 20).build()); y += 25;
            addRenderableWidget(Button.builder(Component.literal("HUD height: " + SunshineConfig.hudH), b -> { SunshineConfig.hudH = SunshineConfig.hudH >= 100 ? 30 : SunshineConfig.hudH + 10; SunshineConfig.save(); b.setMessage(Component.literal("HUD height: " + SunshineConfig.hudH)); }).bounds(x, y, w, 20).build()); y += 25;
        } else if (module.equals("Coordinates")) {
            addRenderableWidget(Button.builder(Component.literal("Coordinates panel width: " + SunshineConfig.coordsW), b -> { SunshineConfig.coordsW = SunshineConfig.coordsW >= 260 ? 100 : SunshineConfig.coordsW + 20; SunshineConfig.save(); b.setMessage(Component.literal("Coordinates panel width: " + SunshineConfig.coordsW)); }).bounds(x, y, w, 20).build());
        } else if (module.equals("Keystrokes")) {
            addRenderableWidget(Button.builder(Component.literal("Keystrokes width: " + SunshineConfig.keysW), b -> { SunshineConfig.keysW = SunshineConfig.keysW >= 150 ? 48 : SunshineConfig.keysW + 10; SunshineConfig.save(); b.setMessage(Component.literal("Keystrokes width: " + SunshineConfig.keysW)); }).bounds(x, y, w, 20).build()); y += 25;
            addRenderableWidget(Button.builder(Component.literal("Keystrokes height: " + SunshineConfig.keysH), b -> { SunshineConfig.keysH = SunshineConfig.keysH >= 120 ? 40 : SunshineConfig.keysH + 10; SunshineConfig.save(); b.setMessage(Component.literal("Keystrokes height: " + SunshineConfig.keysH)); }).bounds(x, y, w, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Enabled: " + moduleEnabled()), b -> { toggleModule(); SunshineConfig.save(); b.setMessage(Component.literal("Enabled: " + moduleEnabled())); }).bounds(x, y, w, 20).build());
            y += 25;
            addRenderableWidget(Button.builder(Component.literal("Open HUD editor"), b -> Minecraft.getInstance().gui.setScreen(new SunshineHudEditorScreen(this))).bounds(x, y, w, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("DONE  •  SAVE"), b -> Minecraft.getInstance().gui.setScreen(parent)).bounds(left + 18, top + panelH - 32, panelW - 36, 22).build());
    }


    private void refreshMotionBlurButtons() {
        for (var widget : children()) {
            if (widget instanceof Button button) {
                String current = button.getMessage().getString();
                if (current.startsWith("Strength:")) {
                    if (current.contains("(-10)")) button.setMessage(Component.literal("Strength: " + SunshineConfig.motionBlurStrength + "%  (-10)"));
                    else if (current.contains("(+1)")) button.setMessage(Component.literal("Strength: " + SunshineConfig.motionBlurStrength + "%  (+1)"));
                    else if (current.contains("(+10)")) button.setMessage(Component.literal("Strength: " + SunshineConfig.motionBlurStrength + "%  (+10)"));
                }
            }
        }
    }

    private boolean moduleEnabled() {
        return switch (module) {
            case "Motion Blur" -> SunshineConfig.motionBlurEnabled;
            case "HUD Overlay" -> SunshineConfig.hud; case "Coordinates" -> SunshineConfig.coords;
            case "Keystrokes" -> SunshineConfig.keystrokes; case "Sprint Indicator" -> SunshineConfig.sprintIndicator;
            case "Player Nametags", "Self Nametag" -> SunshineConfig.selfNametag; case "Freelook" -> SunshineConfig.freelookEnabled;
            case "Clock HUD" -> SunshineConfig.clockHud; case "Direction HUD" -> SunshineConfig.directionHud;
            case "Session Timer" -> SunshineConfig.sessionTimerHud; case "Armor HUD" -> SunshineConfig.armorHud;
            case "Potion Effects", "Potion Effects HUD" -> SunshineConfig.potionHud; case "Control Hints" -> SunshineConfig.hints;
            case "Toggle Sprint" -> SunshineConfig.toggleSprint; case "Mouse Visualizer" -> SunshineConfig.mouseVisualizer;
            case "Left Mouse Button" -> SunshineConfig.leftMouse; case "Right Mouse Button" -> SunshineConfig.rightMouse; case "Attack Cooldown" -> SunshineConfig.attackCooldownHud; case "Target Info" -> SunshineConfig.targetInfoHud; default -> false;
        };
    }
    private void toggleModule() {
        switch (module) {
            case "Motion Blur" -> SunshineClient.toggleMotionBlur();
            case "HUD Overlay" -> SunshineClient.toggleHud(); case "Coordinates" -> SunshineClient.toggleCoords();
            case "Keystrokes" -> SunshineClient.toggleKeystrokes(); case "Sprint Indicator" -> SunshineClient.toggleSprintIndicator();
            case "Player Nametags", "Self Nametag" -> SunshineClient.toggleSelfNametag(); case "Freelook" -> SunshineClient.toggleFreelook();
            case "Clock HUD" -> SunshineClient.toggleClockHud(); case "Direction HUD" -> SunshineClient.toggleDirectionHud();
            case "Session Timer" -> SunshineClient.toggleSessionTimerHud(); case "Armor HUD" -> SunshineClient.toggleArmorHud();
            case "Potion Effects", "Potion Effects HUD" -> SunshineClient.togglePotionHud(); case "Control Hints" -> SunshineClient.toggleHints();
            case "Toggle Sprint" -> SunshineClient.toggleSprintMode(); case "Mouse Visualizer" -> SunshineClient.toggleMouseVisualizer();
            case "Left Mouse Button" -> SunshineClient.toggleLeftMouse(); case "Right Mouse Button" -> SunshineClient.toggleRightMouse(); case "Attack Cooldown" -> SunshineClient.toggleAttackCooldownHud(); case "Target Info" -> SunshineClient.toggleTargetInfoHud();
        }
    }
    private static int nextColor(int c) { int[] colors = {0xFFFFD447, 0xFFFF5555, 0xFF55FFFF, 0xFF55FF55, 0xFFFFFFFF, 0xFFFF55FF}; for (int i=0;i<colors.length;i++) if(colors[i]==c) return colors[(i+1)%colors.length]; return colors[0]; }
    private static String colorName(int c) { return switch(c) { case 0xFFFFD447 -> "Sunshine Gold"; case 0xFFFF5555 -> "Red"; case 0xFF55FFFF -> "Cyan"; case 0xFF55FF55 -> "Green"; case 0xFFFFFFFF -> "White"; case 0xFFFF55FF -> "Magenta"; default -> "Custom"; }; }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        g.fillGradient(0, 0, width, height, 0xFF17100A, 0xFF050609);
        g.fill(left - 2, top - 2, left + panelW + 2, top + panelH + 2, 0xFFFFD447);
        g.fill(left, top, left + panelW, top + panelH, 0xF20B0D11);
        g.fill(left, top, left + panelW, top + 4, GOLD);
        g.outline(left, top, panelW, panelH, 0xFF6E5B25);
        g.text(font, Component.literal("☀ SUNSHINE  /  MODULE SETTINGS").withStyle(s -> s.withColor(GOLD).withBold(true)), left + 18, top + 16, 0xFFFFFFFF);
        g.text(font, Component.literal(module.toUpperCase()), left + 18, top + 39, 0xFFF4F6FA);
        g.text(font, Component.literal("Changes save automatically to sunshine.json"), left + 18, top + 55, 0xFF9EA7B5);
        super.extractRenderState(g, mx, my, partialTick);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
    @Override public void removed() { SunshineConfig.save(); super.removed(); }
}
