package dev.sunshine.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Sunshine ClickGUI: category filtering, search, animated switches, scrolling and touch-friendly toggles. */
public final class SunshineScreen extends Screen {
    private static final int YELLOW = 0xFFFFD447;
    private static final int PALE_GOLD = 0xFFFFEAA0;
    private static final int GRIM_ACCENT = 0xFFB18CFF;
    private static final int GRIM_PALE = 0xFFE0D0FF;
    private static final int TEXT = 0xFFF4F6FA;
    private static final int MUTED = 0xFF9EA7B5;
    private static final int PANEL = 0xF20B0D11;
    private static final int CARD = 0xEE171A20;
    private static final int CARD_HOVER = 0xFF252A32;
    private static final int BORDER = 0xFF3A414C;
    private static final String[] CATEGORIES = {"Visual", "HUD", "Movement", "Combat", "Misc", "Client Looks"};
    private static final int PAD = 14, CLOSE_W = 64, CLOSE_H = 18;

    private final Screen parent;
    private String selectedCategory;
    private EditBox searchBox;
    private int panelX, panelY, panelWidth, panelHeight, contentX, contentWidth, sidebarWidth;
    private int cardsY, cardH, cardGap, catTop, catStep, catH, footerY, cardWidth, columnGap;
    private boolean compact;
    private int scroll;
    private String pendingToggle;
    private double dragAccum;
    private boolean dragged;
    private final Map<String, Float> hoverAnim = new HashMap<>();
    private final Map<String, Float> switchAnim = new HashMap<>();
    private long lastFrame = System.nanoTime();

    private record ModuleInfo(String name, String description, String category, boolean enabled, Runnable toggle) { }

    public SunshineScreen(Screen parent) {
        super(Component.literal("Sunshine Client"));
        this.parent = parent;
        this.selectedCategory = isCategory(SunshineConfig.category) ? SunshineConfig.category : "HUD";
    }

    private int accent() { return SunshineConfig.grimSkidTheme ? GRIM_ACCENT : YELLOW; }
    private int paleAccent() { return SunshineConfig.grimSkidTheme ? GRIM_PALE : PALE_GOLD; }

    private static boolean isCategory(String c) {
        for (String s : CATEGORIES) if (s.equals(c)) return true;
        return false;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(900, this.width - 18);
        panelHeight = Math.min(470, this.height - 18);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        compact = panelHeight < 280;
        sidebarWidth = Math.max(126, Math.min(174, panelWidth / 5));
        contentX = panelX + sidebarWidth + PAD;
        contentWidth = panelWidth - sidebarWidth - PAD * 2;
        footerY = panelY + panelHeight - CLOSE_H - 8;
        catTop = panelY + (compact ? 56 : 64);
        catStep = Math.max(16, Math.min(28, (footerY - 8 - catTop) / CATEGORIES.length));
        catH = catStep - 4;
        cardH = compact ? 38 : (SunshineConfig.grimSkidTheme ? 50 : 58);
        cardGap = compact ? 5 : (SunshineConfig.grimSkidTheme ? 7 : 9);
        columnGap = SunshineConfig.grimSkidTheme ? 8 : 10;
        cardWidth = SunshineConfig.grimSkidTheme ? Math.max(120, (contentWidth - columnGap) / 2) : contentWidth;
        int searchY = panelY + (compact ? 42 : 54);
        int searchH = compact ? 18 : 22;
        cardsY = searchY + searchH + (compact ? 7 : 10);

        String previous = searchBox == null ? "" : searchBox.getValue();
        searchBox = new EditBox(this.font, contentX, searchY, contentWidth, searchH, Component.literal("Search modules"));
        searchBox.setHint(Component.literal("Search modules..."));
        searchBox.setMaxLength(48);
        searchBox.setValue(previous);
        searchBox.setResponder(v -> scroll = 0);
        addRenderableWidget(searchBox);
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Keybinds"), b -> Minecraft.getInstance().gui.setScreen(new SunshineKeybindScreen(this)))
                .bounds(panelX + panelWidth - 86, panelY + 10, 74, 20).build());
    }

    private List<ModuleInfo> allModules() {
        List<ModuleInfo> m = new ArrayList<>();
        m.add(new ModuleInfo("HUD Overlay", "Sunshine title, FPS and CPS panel", "Visual", SunshineConfig.hud, SunshineClient::toggleHud));
        m.add(new ModuleInfo("Coordinates", "Live XYZ position display", "HUD", SunshineConfig.coords, SunshineClient::toggleCoords));
        m.add(new ModuleInfo("Keystrokes", "WASD and SPACE display", "HUD", SunshineConfig.keystrokes, SunshineClient::toggleKeystrokes));
        m.add(new ModuleInfo("Control Hints", "Show keybind hints in the HUD panel", "Misc", SunshineConfig.hints, SunshineClient::toggleHints));
        m.add(new ModuleInfo("Sprint Indicator", "Show a small indicator while sprint is held", "HUD", SunshineConfig.sprintIndicator, SunshineClient::toggleSprintIndicator));
        m.add(new ModuleInfo("Clock HUD", "Show a live 24-hour clock in the top-right", "HUD", SunshineConfig.clockHud, SunshineClient::toggleClockHud));
        m.add(new ModuleInfo("Direction HUD", "Show compass direction and yaw", "HUD", SunshineConfig.directionHud, SunshineClient::toggleDirectionHud));
        m.add(new ModuleInfo("Session Timer", "Show time since the client session began", "HUD", SunshineConfig.sessionTimerHud, SunshineClient::toggleSessionTimerHud));
        m.add(new ModuleInfo("Armor HUD", "Show equipped armor durability and warn when low", "HUD", SunshineConfig.armorHud, SunshineClient::toggleArmorHud));
        m.add(new ModuleInfo("Potion Effects", "Show active potion effects and remaining time", "HUD", SunshineConfig.potionHud, SunshineClient::togglePotionHud));
        m.add(new ModuleInfo("Self Nametag", "Show your own nametag", "Visual", SunshineConfig.selfNametag, SunshineClient::toggleSelfNametag));
        m.add(new ModuleInfo("Motion Blur", "Lunar-like screen-space motion blur (1–100%)", "Visual", SunshineConfig.motionBlurEnabled, SunshineClient::toggleMotionBlur));
        m.add(new ModuleInfo("Freelook", "Toggle freelook setting (camera hook still required)", "Movement", SunshineConfig.freelookEnabled, SunshineClient::toggleFreelook));
        m.add(new ModuleInfo("Potion Effects HUD", "Show active effects and remaining time", "HUD", SunshineConfig.potionHud, SunshineClient::togglePotionHud));
        m.add(new ModuleInfo("Toggle Sprint", "Toggle sprint with J while eligible", "Movement", SunshineConfig.toggleSprint, SunshineClient::toggleSprintMode));
        m.add(new ModuleInfo("Attack Cooldown", "Show your attack charge progress", "Combat", SunshineConfig.attackCooldownHud, SunshineClient::toggleAttackCooldownHud));
        m.add(new ModuleInfo("Target Info", "Show the entity you are aiming at and its distance", "Combat", SunshineConfig.targetInfoHud, SunshineClient::toggleTargetInfoHud));
        m.add(new ModuleInfo("Mouse Visualizer", "Show LMB/RMB buttons beside keystrokes", "HUD", SunshineConfig.mouseVisualizer, SunshineClient::toggleMouseVisualizer));
        m.add(new ModuleInfo("Left Mouse Button", "Show the left mouse button indicator", "HUD", SunshineConfig.leftMouse, SunshineClient::toggleLeftMouse));
        m.add(new ModuleInfo("Right Mouse Button", "Show the right mouse button indicator", "HUD", SunshineConfig.rightMouse, SunshineClient::toggleRightMouse));
        m.add(new ModuleInfo("Visual Profile", "Switch Quality / Balanced (Performance removed)", "Misc", true, SunshineClient::cyclePerformanceMode));
        m.add(new ModuleInfo("Grim Skid Theme", "Switch the ClickGUI to a dark purple Grim-inspired look", "Client Looks", SunshineConfig.grimSkidTheme, () -> { SunshineConfig.grimSkidTheme = !SunshineConfig.grimSkidTheme; SunshineConfig.save(); }));
        return m;
    }

    private List<ModuleInfo> filteredModules() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<ModuleInfo> result = new ArrayList<>();
        for (ModuleInfo module : allModules()) {
            if (!module.category().equals(selectedCategory)) continue;
            if (!query.isEmpty() && !(module.name().toLowerCase(Locale.ROOT).contains(query)
                    || module.description().toLowerCase(Locale.ROOT).contains(query))) continue;
            result.add(module);
        }
        return result;
    }

    private int visibleCardCount() {
        return Math.max(1, (footerY - 8 - cardsY + cardGap) / (cardH + cardGap));
    }

    private int columns() { return SunshineConfig.grimSkidTheme ? 2 : 1; }
    private int rowCount(int totalModules) { return (totalModules + columns() - 1) / columns(); }
    private int maxScroll(int totalModules) { return Math.max(0, rowCount(totalModules) - visibleCardCount()); }
    private int maxScrollRows(int totalRows) { return Math.max(0, totalRows - visibleCardCount()); }
    private int cardX(int moduleIndex) { return contentX + (moduleIndex % columns()) * (cardWidth + columnGap); }
    private int cardY(int moduleIndex) { return cardsY + ((moduleIndex / columns()) - scroll) * (cardH + cardGap); }

    private float animate(Map<String, Float> map, String key, boolean on, float step) {
        float v = SunshineClient.approach(map.getOrDefault(key, on ? 1f : 0f), on ? 1f : 0f, step);
        map.put(key, v);
        return v;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        boolean balanced = SunshineConfig.performanceMode == 1;
        float step = balanced ? 0f : Math.min(0.1f, (now - lastFrame) / 1_000_000_000f) * 9f;
        lastFrame = now;

        boolean grimTheme = SunshineConfig.grimSkidTheme;
        if (grimTheme) {
            // Grim 4.4-inspired presentation: neutral charcoal surfaces, restrained purple
            // selection bars, and quiet borders. Layout and branding remain Sunshine-owned.
            g.fillGradient(0, 0, width, height, 0xFF202226, 0xFF17191D);
            g.fill(panelX - 4, panelY - 4, panelX + panelWidth + 4, panelY + panelHeight + 4, 0x18202024);
            g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF2222429);
            g.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 42, 0xFF25272D);
            g.fill(panelX + 1, panelY + 42, panelX + panelWidth - 1, panelY + 43, 0xFF393C44);
        } else if (balanced) {
            // Balanced intentionally keeps the original, simpler Sunshine presentation.
            g.fillGradient(0, 0, width, height, 0xFFFFD447, 0xFF050609);
            g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF20B0D11);
        } else {
            // QUALITY: a full client workspace, not a gradient-only reskin. Layered surfaces,
            // broad ambient light, a dedicated sidebar rail, and clear header/content separation.
            g.fillGradient(0, 0, width, height, 0xFF3A2A08, 0xFF030406);
            g.fillGradient(0, 0, width, height / 2, 0x663D2D0B, 0x00050609);
            int glowX = (int)((now / 18_000_000L) % Math.max(1, width + 260)) - 130;
            g.fill(glowX, 0, Math.min(width, glowX + 190), height, 0x06FFE38A);
            g.fill(panelX - 8, panelY - 8, panelX + panelWidth + 8, panelY + panelHeight + 8, 0x15FFD447);
            g.fill(panelX - 4, panelY - 4, panelX + panelWidth + 4, panelY + panelHeight + 4, 0x24FFD447);
            g.fillGradient(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF21A1C21, 0xF207080B);
            g.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 42, SunshineConfig.grimSkidTheme ? 0xFF24202F : 0xFF24231E);
            g.fill(panelX + 1, panelY + 42, panelX + panelWidth - 1, panelY + 43, accent());
            g.fill(panelX + 1, panelY + panelHeight - 24, panelX + panelWidth - 1, panelY + panelHeight - 23, 0xFF303138);
            // A wide moving amber sheen gives the Quality profile depth without a lone animated line.
            int sheenX = panelX + 1 + (int)((now / 34_000_000L) % Math.max(1, panelWidth - 120));
            g.fillGradient(sheenX, panelY + 1, Math.min(panelX + panelWidth - 1, sheenX + 120), panelY + 3, 0x00FFD447, 0x66FFD447);
        }
        g.fill(panelX, panelY, panelX + panelWidth, panelY + (grimTheme ? 2 : 3), accent());
        g.outline(panelX, panelY, panelWidth, panelHeight, grimTheme ? 0xFF484B53 : (balanced ? BORDER : 0xFF82703B));
        g.fillGradient(panelX + 1, panelY + 3, panelX + sidebarWidth, panelY + panelHeight - 1,
                grimTheme ? 0xDD25282E : (balanced ? 0xB8090B0F : 0xDD12130F), grimTheme ? 0xDD22252A : 0xF207080A);
        g.fill(panelX + sidebarWidth, panelY + 3, panelX + sidebarWidth + 1, panelY + panelHeight - 1,
                balanced ? 0xFF303640 : (SunshineConfig.grimSkidTheme ? 0xFF3A3D44 : 0xFF65532C));

        if (!balanced && !grimTheme) {
            g.fillGradient(panelX + PAD, panelY + 8, panelX + PAD + 28, panelY + 36, 0xFF5B4510, 0xFF19170F);
            g.outline(panelX + PAD, panelY + 8, 28, 28, grimTheme ? GRIM_ACCENT : 0xFFB2943D);
            g.centeredText(font, Component.literal("☀").withStyle(s -> s.withColor(paleAccent()).withBold(true)), panelX + PAD + 14, panelY + 18, TEXT);
            g.text(font, Component.literal("SUNSHINE CLIENT").withStyle(s -> s.withColor(accent()).withBold(true)), panelX + PAD + 36, panelY + 10, TEXT);
            g.text(font, Component.literal("YOUR MODS. YOUR LAYOUT. YOUR STYLE.").withStyle(s -> s.withColor(MUTED)), panelX + PAD + 36, panelY + 24, TEXT);
            int pillX = panelX + panelWidth - 126;
            g.fillGradient(pillX, panelY + 10, pillX + 112, panelY + 29, grimTheme ? 0xFF373A41 : 0xFF302817, grimTheme ? 0xFF2B2E34 : 0xFF171811);
            g.outline(pillX, panelY + 10, 112, 19, grimTheme ? 0xFF70558F : 0xFF77602E);
            g.fill(pillX + 8, panelY + 17, pillX + 13, panelY + 22, 0xFF65E39A);
            g.text(font, Component.literal("CLIENT ACTIVE").withStyle(s -> s.withColor(0xFFD8E8D6).withBold(true)), pillX + 19, panelY + 16, TEXT);
        } else if (grimTheme) {
            g.fill(panelX + PAD, panelY + 10, panelX + PAD + 22, panelY + 30, 0xFF34313D);
            g.outline(panelX + PAD, panelY + 10, 22, 20, 0xFF554967);
            g.centeredText(font, Component.literal("S").withStyle(s -> s.withColor(GRIM_PALE).withBold(true)), panelX + PAD + 11, panelY + 16, TEXT);
            g.text(font, Component.literal("SUNSHINE").withStyle(s -> s.withColor(TEXT).withBold(true)), panelX + PAD + 30, panelY + 10, TEXT);
            g.text(font, Component.literal("CLIENT  /  4.4-INSPIRED THEME").withStyle(s -> s.withColor(MUTED)), panelX + PAD + 30, panelY + 23, TEXT);
        } else {
            g.text(font, Component.literal("SUNSHINE").withStyle(s -> s.withColor(accent()).withBold(true)), panelX + PAD, panelY + 12, TEXT);
            g.text(font, Component.literal("CLIENT  /  P5  ·  BALANCED").withStyle(s -> s.withColor(MUTED)), panelX + PAD, panelY + 23, TEXT);
        }
        g.text(font, Component.literal(grimTheme ? "FEATURES" : "CATEGORIES").withStyle(s -> s.withColor(MUTED).withBold(true)), panelX + PAD, catTop - 12, TEXT);

        int categoryY = catTop;
        for (String category : CATEGORIES) {
            boolean selected = category.equals(selectedCategory);
            boolean hover = inside(mouseX, mouseY, panelX + 8, categoryY, sidebarWidth - 16, catH);
            float h = balanced ? (selected ? 1f : hover ? 0.35f : 0f)
                    : animate(hoverAnim, "cat:" + category, hover || selected, step);
            int bg = grimTheme
                    ? SunshineClient.lerpColor(0x002E3137, selected ? 0xFF34373E : 0x55383D46, h)
                    : SunshineClient.lerpColor(0x00333322, selected ? 0x66333322 : 0x553F4652, h);
            g.fill(panelX + 8, categoryY, panelX + sidebarWidth - 8, categoryY + catH, bg);
            if (selected) g.fill(panelX + 8, categoryY, panelX + 11, categoryY + catH, accent());
            int offset = Math.round(h * 3);
            g.text(font, Component.literal(category).withStyle(s -> s.withColor(selected ? accent() : TEXT)),
                    panelX + 17 + offset, categoryY + (catH - 8) / 2, TEXT);
            categoryY += catStep;
        }

        List<ModuleInfo> modules = filteredModules();
        scroll = Math.max(0, Math.min(scroll, maxScroll(modules.size())));
        g.text(font, Component.literal(selectedCategory.toUpperCase(Locale.ROOT))
                .withStyle(s -> s.withColor(accent()).withBold(true)), contentX, panelY + 12, TEXT);
        g.text(font, Component.literal(modules.size() + " MODULE" + (modules.size() == 1 ? "" : "S") + "  /  CLICK TO TOGGLE")
                .withStyle(s -> s.withColor(MUTED)), contentX, panelY + 24, TEXT);
        if (grimTheme) {
            g.fill(contentX, panelY + 39, contentX + contentWidth, panelY + 40, 0xFF383B42);
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);

        int y = cardsY;
        if (modules.isEmpty()) {
            int h = Math.min(60, footerY - 8 - cardsY);
            g.fill(contentX, y, contentX + contentWidth, y + h, CARD);
            g.outline(contentX, y, contentWidth, h, BORDER);
            String message = searchBox != null && !searchBox.getValue().isBlank()
                    ? "No matching modules" : "No modules in this category yet";
            g.centeredText(font, Component.literal(message).withStyle(s -> s.withColor(MUTED)),
                    contentX + contentWidth / 2, y + (h - 8) / 2, TEXT);
        } else {
            int first = scroll * columns();
            int end = Math.min(modules.size(), (scroll + visibleCardCount()) * columns());
            for (int i = first; i < end; i++) {
                int x = cardX(i);
                int rowY = cardY(i);
                drawCard(g, modules.get(i), x, rowY, cardWidth, mouseX, mouseY, step);
            }
            if (maxScroll(modules.size()) > 0) drawScrollbar(g, rowCount(modules.size()));
        }

        g.fill(panelX + 1, footerY - 6, panelX + panelWidth - 1, footerY - 5, 0xFF262B33);
        int footerTextY = footerY + (CLOSE_H - 8) / 2;
        g.text(font, Component.literal("LMB " + SunshineClient.getLeftCps() + "  RMB " + SunshineClient.getRightCps())
                .withStyle(s -> s.withColor(MUTED)), panelX + PAD, footerTextY, TEXT);
        String closeHint = "RSHIFT / ESC  ·  CLOSE";
        if (contentX + font.width(closeHint) + 8 < closeX()) {
            g.text(font, Component.literal(closeHint).withStyle(s -> s.withColor(MUTED)), contentX, footerTextY, TEXT);
        }
        drawCloseButton(g, mouseX, mouseY, step);
        // Keep EDIT HUD in the sidebar header; the old position collided with Keybinds.
        int ex = panelX + PAD, ey = panelY + 34;
        boolean editHover = inside(mouseX, mouseY, ex, ey, 78, 18);
        if (!balanced) {
            int glow = grimTheme ? 0x447E5BB0 : 0x66FFD447;
            g.fill(ex - 1, ey - 1, ex + 79, ey + 19, editHover ? glow : 0x33222222);
        }
        int editFill = grimTheme ? (editHover ? 0xFF393344 : 0xFF292C32) : (editHover ? 0xFF39311C : 0xFF20242B);
        g.fill(ex, ey, ex + 78, ey + 18, editFill);
        g.outline(ex, ey, 78, 18, editHover ? accent() : BORDER);
        g.centeredText(font, Component.literal("EDIT HUD").withStyle(st -> st.withColor(editHover ? accent() : TEXT)), ex + 39, ey + 5, TEXT);
    }

    private void drawCard(GuiGraphicsExtractor g, ModuleInfo module, int x, int y, int w, int mouseX, int mouseY, float step) {
        boolean hover = inside(mouseX, mouseY, x, y, w, cardH);
        boolean balanced = SunshineConfig.performanceMode == 1;
        float h = balanced ? (hover ? 0.35f : 0f) : animate(hoverAnim, "card:" + module.name(), hover, step);
        float s = balanced ? (module.enabled() ? 1f : 0f)
                : animate(switchAnim, module.name(), module.enabled(), step * 1.4f);
        if (SunshineConfig.grimSkidTheme) {
            g.fill(x, y, x + w, y + cardH, SunshineClient.lerpColor(0xFF292C32, 0xFF343740, h));
            g.outline(x, y, w, cardH, SunshineClient.lerpColor(0xFF41454E, 0xFF625671, h));
            // Narrow activation rail: neutral when disabled, muted violet when enabled.
            g.fill(x, y + 1, x + 3, y + cardH - 1, SunshineClient.lerpColor(0xFF555A63, GRIM_ACCENT, s));
        } else if (balanced) {
            g.fill(x, y, x + w, y + cardH, SunshineClient.lerpColor(CARD, CARD_HOVER, h));
            g.outline(x, y, w, cardH, SunshineClient.lerpColor(BORDER, 0xFF8A7A3E, h));
        } else {
            g.fill(x - 2, y - 1, x + w + 2, y + cardH + 1, h > 0.15f ? 0x28FFD447 : 0x08000000);
            g.fillGradient(x, y, x + w, y + cardH,
                    SunshineClient.lerpColor(0xFF25262A, 0xFF3C321A, h),
                    SunshineClient.lerpColor(0xFF15171B, 0xFF211C10, h));
            g.outline(x, y, w, cardH, SunshineClient.lerpColor(0xFF454A53, 0xFFC5A447, h));
            g.fill(x + 1, y + 1, x + 4, y + cardH - 1, SunshineClient.lerpColor(0xFF666B73, accent(), s));
            g.fill(x + 10, y + 8, x + 32, y + cardH - 8, 0xFF28271F);
            g.outline(x + 10, y + 8, 22, cardH - 16, 0xFF4B432C);
        }
        if (!SunshineConfig.grimSkidTheme) {
            g.fill(x, y, x + 3, y + cardH, SunshineClient.lerpColor(0xFF555C68, accent(), s));
        }
        int nameY = y + (compact ? 5 : (SunshineConfig.grimSkidTheme ? 8 : 10));
        int textX = x + (balanced ? 12 : (SunshineConfig.grimSkidTheme ? 12 : 40));
        if (!balanced && !SunshineConfig.grimSkidTheme) g.centeredText(font, Component.literal(module.name().substring(0, 1)).withStyle(st -> st.withColor(accent()).withBold(true)), x + 21, y + (cardH - 8) / 2, TEXT);
        g.text(font, Component.literal(module.name()).withStyle(st -> st.withColor(TEXT).withBold(true)), textX, nameY, TEXT);
        g.text(font, Component.literal(module.description()).withStyle(st -> st.withColor(MUTED)), textX, nameY + (compact ? 11 : (SunshineConfig.grimSkidTheme ? 13 : 15)), TEXT);
        int gearX = x + w - 66;
        g.fill(gearX, y + (cardH - 18) / 2, gearX + 20, y + (cardH - 18) / 2 + 18, hover ? (SunshineConfig.grimSkidTheme ? 0xFF403749 : 0xFF38301A) : 0xFF25282E);
        g.outline(gearX, y + (cardH - 18) / 2, 20, 18, hover ? accent() : (SunshineConfig.grimSkidTheme ? 0xFF4A4E57 : BORDER));
        g.centeredText(font, Component.literal("⚙").withStyle(st -> st.withColor(hover ? accent() : MUTED)), gearX + 10, y + (cardH - 8) / 2, TEXT);
        if (!balanced && !SunshineConfig.grimSkidTheme) {
            int tagX = x + w - 112;
            int tagY = y + (cardH - 15) / 2;
            g.fill(tagX, tagY, tagX + 34, tagY + 15, module.enabled() ? 0xFF25372A : 0xFF292B30);
            g.outline(tagX, tagY, 34, 15, module.enabled() ? 0xFF477B52 : 0xFF444953);
            g.centeredText(font, Component.literal(module.enabled() ? "ON" : "OFF").withStyle(st -> st.withColor(module.enabled() ? 0xFF8BE6A1 : MUTED).withBold(true)), tagX + 17, tagY + 4, TEXT);
        }
        drawSwitch(g, x + w - 34, y + (cardH - 14) / 2, s);
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int total) {
        int trackTop = cardsY, trackBottom = footerY - 10;
        int x = contentX + contentWidth + 4;
        int trackH = trackBottom - trackTop;
        int thumbH = Math.max(12, trackH * visibleCardCount() / total);
        int thumbY = trackTop + (trackH - thumbH) * scroll / Math.max(1, maxScrollRows(total));
        g.fill(x, trackTop, x + 2, trackBottom, 0xFF262B33);
        g.fill(x, thumbY, x + 2, thumbY + thumbH, accent());
    }

    private void drawSwitch(GuiGraphicsExtractor g, int x, int y, float t) {
        int w = SunshineConfig.grimSkidTheme ? 26 : 30, h = SunshineConfig.grimSkidTheme ? 14 : 16;
        int onColor = SunshineConfig.grimSkidTheme ? GRIM_ACCENT : 0xFFFFC928;
        int onBorder = SunshineConfig.grimSkidTheme ? GRIM_PALE : 0xFFFFE58A;
        g.fill(x, y, x + w, y + h, SunshineClient.lerpColor(0xFF424854, onColor, t));
        g.outline(x, y, w, h, SunshineClient.lerpColor(0xFF687180, onBorder, t));
        int knobX = x + 2 + Math.round(t * (w - 16));
        g.fill(knobX, y + 2, knobX + 10, y + h - 2, SunshineClient.lerpColor(0xFFE5E8ED, 0xFF15171B, t));
    }

    private int closeX() { return panelX + panelWidth - PAD - CLOSE_W; }
    private int closeY() { return footerY; }

    private void drawCloseButton(GuiGraphicsExtractor g, int mx, int my, float step) {
        int x = closeX(), y = closeY();
        boolean hovered = inside(mx, my, x, y, CLOSE_W, CLOSE_H);
        float h = SunshineConfig.performanceMode == 1 ? (hovered ? 1f : 0f)
                : animate(hoverAnim, "close", hovered, step);
        g.fill(x, y, x + CLOSE_W, y + CLOSE_H, SunshineClient.lerpColor(0xFF20242B, 0xFF383E48, h));
        g.outline(x, y, CLOSE_W, CLOSE_H, SunshineClient.lerpColor(BORDER, accent(), h));
        g.centeredText(font, Component.literal("CLOSE").withStyle(s -> s.withColor(h > 0.5f ? accent() : TEXT)),
                x + CLOSE_W / 2, y + (CLOSE_H - 8) / 2, TEXT);
    }

    private ModuleInfo cardAt(double mx, double my) {
        List<ModuleInfo> modules = filteredModules();
        int first = scroll * columns(), end = Math.min(modules.size(), (scroll + visibleCardCount()) * columns());
        for (int i = first; i < end; i++) {
            if (inside(mx, my, cardX(i), cardY(i), cardWidth, cardH)) return modules.get(i);
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        if (event.button() == 0) {
            int categoryY = catTop;
            for (String category : CATEGORIES) {
                if (inside(mouseX, mouseY, panelX + 8, categoryY, sidebarWidth - 16, catH)) {
                    selectedCategory = category;
                    scroll = 0;
                    SunshineConfig.category = category;
                    SunshineConfig.save();
                    return true;
                }
                categoryY += catStep;
            }
            int ex = panelX + PAD, ey = panelY + 34;
            if (inside(mouseX, mouseY, ex, ey, 78, 18)) {
                Minecraft.getInstance().gui.setScreen(new SunshineHudEditorScreen(this));
                return true;
            }
            if (inside(mouseX, mouseY, closeX(), closeY(), CLOSE_W, CLOSE_H)) {
                onClose();
                return true;
            }
            ModuleInfo card = cardAt(mouseX, mouseY);
            if (card != null) {
                List<ModuleInfo> visibleModules = filteredModules();
                int moduleIndex = 0;
                while (moduleIndex < visibleModules.size() && !visibleModules.get(moduleIndex).name().equals(card.name())) moduleIndex++;
                int cardY = moduleIndex < visibleModules.size() ? cardY(moduleIndex) : cardsY;
                int gearX = moduleIndex < visibleModules.size() ? cardX(moduleIndex) + cardWidth - 66 : contentX;
                if (inside(mouseX, mouseY, gearX, cardY, 20, cardH)) {
                    Minecraft.getInstance().gui.setScreen(new SunshineModuleSettingsScreen(this, card.name()));
                    return true;
                }
                // Toggle on release so touch drags (Android/MJLauncher) can scroll without toggling.
                pendingToggle = card.name();
                dragAccum = 0;
                dragged = false;
                setFocused(null);
                searchBox.setFocused(false);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (pendingToggle != null) {
            dragAccum += dy;
            if (Math.abs(dragAccum) > 4) dragged = true;
            int stepPx = cardH + cardGap;
            while (dragAccum <= -stepPx) { scroll++; dragAccum += stepPx; }
            while (dragAccum >= stepPx) { scroll--; dragAccum -= stepPx; }
            scroll = Math.max(0, Math.min(scroll, maxScroll(filteredModules().size())));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (pendingToggle != null) {
            String name = pendingToggle;
            pendingToggle = null;
            ModuleInfo card = cardAt(event.x(), event.y());
            if (!dragged && card != null && card.name().equals(name)) card.toggle().run();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside(mouseX, mouseY, contentX, cardsY, contentWidth + 8, footerY - cardsY) && scrollY != 0) {
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), maxScroll(filteredModules().size())));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!searchBox.isFocused() && SunshineClient.menuKey.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
}
